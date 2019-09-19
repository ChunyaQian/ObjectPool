package pool;

public class ObjectPoolFactory {
        // 得到对象池的一个单例
    private static ObjectPoolFactory instance = new ObjectPoolFactory();
    private ObjectPoolFactory() {
    }

    public static ObjectPoolFactory getInstance() {
        return instance;
    }

    public ObjectPool createPool(Class clsType,int maxNumOfObject, int minNumOfObject, int maxFreeTime){
        ObjectPool objectPool = new ObjectPool(clsType,maxNumOfObject,minNumOfObject,maxFreeTime);
        objectPool.createPool();
        return objectPool;
    }

    public void closePool(ObjectPool objectPool){
        objectPool.closeObjectPool();
    }
}
