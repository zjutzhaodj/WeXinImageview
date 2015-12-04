package com.example.administrator.myapplication;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Zhaodj on 2015/11/16.
 */
public class MyImageLoader {

    //单例
    private static MyImageLoader minstance;

    //图片缓存
    private LruCache<String, Bitmap> mLruCache;

    //线程池
    private ExecutorService mThreadPool;

    //默认线程数量
    private static final int DEFAULT_THREAD_COUNT = 1;

    //调度方式
    private Type mType = Type.LIFO;

    //任务队列，选择为LinkedList 因为可以直接对链的头部和尾部进行数据操作
    private LinkedList<Runnable> mTaskQueue;

    //轮训线程
    private Thread mPoolThread;

    //轮询线程的handler
    private Handler mPoolThreadHandler;

    //通知更新UI的handler
    private Handler mUIHandler;

    //为保证使用handler前，已经对其初始化完成，而进行的信号量机制
    private Semaphore semaphorePoolThreadHandler = new Semaphore(0);

    //线程数目的信号量
    private Semaphore semaphoreThreadPool;

    //任务队列的调度方式
    public enum Type {
        FIFO, LIFO;
    }


    /**
     * 构造函数
     *
     * @param mThreadCount 总的线程数目
     * @param type         任务的调度方式
     */
    private MyImageLoader(int mThreadCount, Type type) {
        init(mThreadCount, type);
    }


    /**
     * 变量的初始化
     *
     * @param mThreadCount
     * @param type
     */

    private void init(int mThreadCount, Type type) {

        //后台轮训线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                //利用handler message Looper的异步机制来进行任务的轮询
                //Looper会不断去取出 当前Thread的MessageQueue中的Message交给 mPoolThreadHandler去处理
                Looper.prepare();

                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务进行执行
                        try {

                            //获取一个线程池信号量，如果线程池 线程数还没有满，则可以去mTaskQueue取任务，加入到线程池任务队列
                            semaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //取得一个任务，加入到线程池
                        mThreadPool.execute(getTask());
                    }
                };

                //释放一个信号量，确保在使用mPoolThreadHandler的时候，mPoolThreadHandler已经初始化完毕
                semaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };

        mPoolThread.start();


        //获取最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //设定缓存内存大小
        int cacheMemory = maxMemory / 8;

        //初始化缓存
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {

            /**
             * 定义每个bitmap的大小
             * @param key
             * @param value
             * @return
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };


        //初始化线程池，线程数量为mThreadCount
        mThreadPool = Executors.newFixedThreadPool(mThreadCount);
        //初始化我的任务队列
        mTaskQueue = new LinkedList<Runnable>();
        //初始化任务调度方式
        mType = type;
        //初始化线程池信号量
        semaphoreThreadPool = new Semaphore(mThreadCount);

    }


    /**
     * 从任务队列中根据调度方式取任务
     *
     * @return
     */
    private Runnable getTask() {

        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();

        }
        return null;
    }


    /**
     * 获得MyImageLoader的单例
     *
     * @param threadCount
     * @param type
     * @return
     */
    public static MyImageLoader getInstance(int threadCount, Type type) {
        if (minstance == null) {
            synchronized (MyImageLoader.class) {
                if (minstance == null) {
                    minstance = new MyImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return minstance;
    }


    /**
     * 根据路径，为对应的Imagview加载图片
     *
     * @param path
     * @param imageView
     */

    public void loadImage(final String path, final ImageView imageView) {

        //防止图片的错乱
        imageView.setTag(path);

        if (mUIHandler == null) {

            //更新UI的handler
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获取得到的图片，设置图片
                    ImgBeanHolder imgBeanHolder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = imgBeanHolder.bitmap;
                    ImageView imageView1 = imgBeanHolder.imageView;
                    String path = imgBeanHolder.path;

                    if (imageView1.getTag().toString().equals(path)) {
                        imageView1.setImageBitmap(bm);
                    }

                }
            };
        }


        Bitmap bm = getBitmapFromCache(path);

        if (bm != null) {
            refreshBitmap(bm, path, imageView);

            //如果在缓存中找不到该路径下的bitmap，则开启一个任务去加载图片
        } else {

            addTasks(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    ImageSize imageSize = getimageViewSize(imageView);

                    Bitmap bm = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);

                    addBitmapToLruCache(path, bm);
                    refreshBitmap(bm, path, imageView);
                    //释放一个线程池的信号量
                    semaphoreThreadPool.release();

                }
            });
        }

    }


    /**
     * 向UIhandler发送更新UI的信息
     *
     * @param bm
     * @param path
     * @param imageView
     */

    private void refreshBitmap(Bitmap bm, String path, ImageView imageView) {
        Message message = Message.obtain();
        ImgBeanHolder imgBeanHolder = new ImgBeanHolder();
        imgBeanHolder.bitmap = bm;
        imgBeanHolder.path = path;
        imgBeanHolder.imageView = imageView;
        message.obj = imgBeanHolder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 将指定路径的bitmap加载到缓存，path是key ，bm是value
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {

        if (getBitmapFromCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }

        }
    }

    /**
     * 根据路径 需求的宽高加载bitamap
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        //不加载到内存
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, width, height);

        //加载到内存
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * 根据需求的宽和高 和实际的宽和高 计算 insamplesize
     *
     * @param options
     * @param reqwidth
     * @param reqheight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {

        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (width > reqwidth || height > reqheight) {

            int widthRadio = Math.round(width * 1.0f / reqwidth * 1.0f);
            int heightRadio = Math.round(height * 1.0f / reqheight * 1.0f);
            inSampleSize = Math.max(widthRadio, heightRadio);

        }

        return inSampleSize;
    }

    /**
     * 通过反射获取属性的值
     *
     * @param object
     * @param filedname
     * @return
     */
    private static int getImageViewFieldValue(Object object, String filedname) {

        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(filedname);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;

    }

    /**
     * 如果Imageview的尺寸<0 则设置为LayoutParams中的尺寸，
     * 如果用户设置了wrapcontent 或者 mathparent的属性，则设置为Imageview的最大尺寸
     * 如果用户没有设置Imageview的最大尺寸，则让他等于屏幕的尺寸
     * <p>
     * 根据imgview获得适当的尺寸
     *
     * @param imageView
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private ImageSize getimageViewSize(ImageView imageView) {
        //获得屏幕尺寸
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ImageSize imageSize = new ImageSize();
        //获得布局中的尺寸
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();

        if (width <= 0) {
            width = lp.width;
        }
        //wrapcontent =-1；fillparent=-2，检查是否设置最大值
        if (width <= 0) {
            width = imageView.getMaxWidth();
        }

        //如果没有设置最大值，则设置为屏幕的宽度
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }


        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = imageView.getMaxHeight();
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }

        imageSize.height = height;
        imageSize.width = width;
        return imageSize;

    }

    /**
     * @param runnable
     */

    private synchronized void addTasks(Runnable runnable) {
        //添加任务到任务队列
        mTaskQueue.add(runnable);
        try {
            //判断是否要请求一个信号量，如果mPoolThreadHandler==null；则阻塞当前线程，去请求信号量，等待mPoolThreadHandler初始化完毕
            if (mPoolThreadHandler == null)
                semaphorePoolThreadHandler.acquire();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //通知轮询线程去任务队列 取出任务
        mPoolThreadHandler.sendEmptyMessage(0X110);
    }

    /**
     * 从缓存中根据路径获得bitmap
     *
     * @param path
     * @return
     */
    private Bitmap getBitmapFromCache(String path) {

        return mLruCache.get(path);
    }

    private class ImageSize {
        int width;
        int height;
    }

    private class ImgBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
