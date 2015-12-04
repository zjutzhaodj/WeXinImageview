package Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/12/2.
 */
public class FolderBean {
    private String dir;
    private String firstimgPath;
    private String name;
    private int count;
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    public String getDir() {
        return dir;
    }

    public String getFirstimgPath() {
        return firstimgPath;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.indexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public void setFirstimgPath(String firstimgPath) {
        this.firstimgPath = firstimgPath;
    }


    public void setCount(int count) {
        this.count = count;
    }
}
