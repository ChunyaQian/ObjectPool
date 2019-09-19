package pool;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


class ObjectPool implements AbstractPool{
    private  int maxNumOfObject = 10;// 对象池的最大数量
    private  int minNumOfObject = 3;// 对象池的最大数量
    private static int curNumOfObject = 0;
    private int maxFreeTime = 700;
    private static boolean closed =false;
    private Class clsType;
    private static ConcurrentHashMap<Object, PooledObject> objects = null;// 存放对象池中对象的可变向量
    private static ConcurrentLinkedQueue<Object> list = null;

    private final Logger logger = Logger.getLogger(ObjectPool.class.toString());
    private String logPath = ".\\ObjectPool.log";


    public ObjectPool(Class clsType,int maxNumOfObject, int minNumOfObject, int maxFreeTime){
        this.clsType=clsType;
        this.maxNumOfObject = maxNumOfObject;
        this.minNumOfObject = minNumOfObject;
        this.maxFreeTime = maxFreeTime;
        GarbageCollect timeKeeper = new GarbageCollect();
        Thread thread = new Thread(timeKeeper);
        thread.start();
    }

    public void logConfig() throws IOException{
        try {
            FileHandler fileHandler = new FileHandler(logPath, true);
            fileHandler.setLevel(Level.INFO);
            logger.setUseParentHandlers(false);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 创建一个对象池
    public synchronized void createPool() {
        try{
            logConfig();
        }catch (IOException e){
            e.printStackTrace();
        }
        if (objects != null) {
            logger.severe("Error: ObjectPool hasn't been created!");
            return;
        }
        objects = new ConcurrentHashMap<>();
        list = new ConcurrentLinkedQueue<>();

        PoolableObjectFactory objFactory =PoolableObjectFactory.getInstance();
        for (int i = 0; i < minNumOfObject; i++) {
            Object obj = objFactory.createObject(clsType);
            list.offer(obj);
            objects.put(obj, new PooledObject(obj));
            curNumOfObject++;
        }
        logger.info("ObjectPool created successfully!");
    }

    public int getNumActive() {
        return curNumOfObject;
    }

    // 返回对象池中可用的对象
    public Object getUsableObject() {
        // 如果对象池没有创建起来，则是不可能有对象存在的
        if (objects == null) {
            logger.severe("Error: ObjectPool hasn't been created!");
            return null;
        }

        if (list.size() > 0) {
            return findUsableObject();
        }

        if (list.size() == 0 && curNumOfObject < maxNumOfObject) {
            PoolableObjectFactory objFactory =PoolableObjectFactory.getInstance();
            Object obj = objFactory.createObject(clsType);
            curNumOfObject++;
            logger.info("Object " + obj.toString() + " is obtained.");
            return obj;
        }

        Object conn = null;
        long startTime = System.currentTimeMillis();
        long endTime;
        try {
            while (conn == null) {
                conn = findUsableObject(); // 获得一个可用的对象
                endTime = System.currentTimeMillis();
                if (endTime - startTime > 200) {
                    throw new RuntimeException();
                }
            }
        } catch (RuntimeException e) {
            logger.severe("Error: Cannot getUsable Objects in 200ms, getUsableObject failed");
        }
        if (conn != null) {
            logger.info("Object " + conn.toString() + " is obtained.");
            return conn;
        }
        return null;
    }

    // 找到一个可用对象
    private Object findUsableObject() {
        Object obj = list.poll();
        objects.remove(obj);
        return obj;
    }

    // 返回一个对象到对象池，并且标记这个对象可用
    // 也就是是说外部不再使用这个对象了，对象池要回收了，以供其他客户端使用
    public void returnObject(Object obj) {
        if (objects == null) {
            logger.severe("Error: ObjectPool hasn't been created!");
            return;
        }
        if(obj.getClass().equals(clsType)) {
            synchronized (obj) {
                list.offer(obj);
                PooledObject pObj = new PooledObject(obj);
                pObj.setTime(System.currentTimeMillis());
                objects.put(obj, pObj);
            }
            logger.info("Object " + obj.toString() + " is returned.");
        }else {
            logger.severe("Error:Wrong Type!");
        }
    }


    // 关闭对象池，同时清空池中所有对象
    public synchronized void closeObjectPool() {
        // 确保对象池存在，如果不存在，返回
        if (objects == null) {
            return;
        }

        Iterator<Map.Entry<Object, PooledObject>> it = objects.entrySet().iterator();
        Iterator itList = list.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        while (itList.hasNext()){
            itList.next();
            itList.remove();
        }

        // 置对象池为空
        objects = null;
        logger.info("ObjectPool is closed!");
        closed = true;
        return;
    }

    private class GarbageCollect implements Runnable {
        long lastCheckTime = System.currentTimeMillis();

        @Override
        public void run() {
            while (!closed) {
                long cur = System.currentTimeMillis();
                if (cur - lastCheckTime == maxFreeTime / 2) {
                    Iterator it = list.iterator();
                    while (it.hasNext()) {
                        Object obj = it.next();
                        if (cur - objects.get(obj).getLastTime() > maxFreeTime) {
                            it.remove();
                            objects.remove(obj);
                            curNumOfObject--;
                        } else {
                            break;
                        }
                    }
                    if (curNumOfObject < minNumOfObject) {
                        PoolableObjectFactory objFactory =PoolableObjectFactory.getInstance();
                        int addCount = minNumOfObject - curNumOfObject;
                        for (int i = 0; i < addCount; i++) {
                            Object obj = objFactory.createObject(clsType);
                            synchronized (obj) {
                                list.offer(obj);
                                objects.put(obj, new PooledObject(obj));
                                curNumOfObject++;
                            }
                        }
                    }
                    lastCheckTime = cur;
                }
//                System.out.println(curNumOfObject);
            }
        }
    }

}
