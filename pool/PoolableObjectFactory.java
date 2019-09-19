package pool;

public class PoolableObjectFactory {
    private static PoolableObjectFactory instance = new PoolableObjectFactory();
    private PoolableObjectFactory() {
    }

    public static PoolableObjectFactory getInstance() {
        return instance;
    }

    public Object createObject(Class clsType) {
        Object obj = null;
        try {
            obj = clsType.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
