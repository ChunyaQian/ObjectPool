package pool;

public interface AbstractPool {
    public void createPool();
    public Object getUsableObject();
    public void returnObject(Object obj);
    public void closeObjectPool();
}
