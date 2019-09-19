package pool;

// 对象池中存放的类型
// 遵循nested class和top-level class的使用情况
// 这个类只在ObjectPool内部使用，所以最好定义成嵌套类
public class PooledObject {
    private Object object = null;// 对象
    private boolean used = false;// 是否有外部客户端正在使用，默认是没有
    long lastUsedTime = System.currentTimeMillis();

    public PooledObject(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public void setTime(long curTime) {
        this.lastUsedTime = curTime;
    }

    public long getLastTime(){
        return lastUsedTime;
    }
}