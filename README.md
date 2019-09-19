# 设计文档 #
## 目的需求 ##
将用过的对象保存起来，等下一次需要这种对象的时候，再拿出来重复使用，从而在一定程度上减少频繁创建对象所造成的开销。

## 设计组成 ##
1. 对象池工厂类(ObjectPoolFactory)：单例模式，负责管理不同类型的对象池，包括创建和销毁。
2. 池化对象工厂类（PoolableObjectFactory）:单例模式，负责生成不同类型的对象。
3. 对象池接口（AbstractPool）: 定义单个对象池功能组成。
4. 对象池类（ObjectPool）： 外部调用类库，具体实现AbstractPool，管理同等类型的对象池。负责对象池参数的初始化、对象池的创建和销毁、管理要被池化对象的借出和归还、空闲对象回收。
5.  池化对象类（PooledObject）：外部对象在对象池内部的存储形式即池化对象，附加上次修改时间lastUsedTimed,used等属性方便在对象池中使用。
![](/Images/UML.jpg)

## 操作方案 ##
为了保证线程安全，采用ConcurrentHashMap和ConcurrentLinkedQueue结构。ConcurrentLinkedQueue里保存原来对象，旧对象在头节点取得时候弹出，新对象在尾节点还回。

获取对象池可用对象，如果ConcurrentLinkedQueue里不为空，说明有可用对象，弹出队头。如果为空，现有对象总数小于最大对象数（maxNumOfObject）,新生成一个对象给外部使用；若现有对象总数大于最大对象数（maxNumOfObject），只能等待有对象归还，超时200ms未获取到抛出异常。

归还对象时先检测类型是否正确，然后在ConcurrentLinkedQueue队尾插入并更新ConcurrentHashMap。

回收最大空闲对象采用定时线程。每隔一半最大空闲时间就进行一次对象回收，前提是对象池未关闭，关闭时线程要自动结束。从队头开始遍历，如果大于最大空闲时间则回收对象。回收结束后，若现有对象数小于minNumOfObject，还需重新生成补齐对象数。



# 接口文档 #
## 对象池接口 ##
    public interface AbstractPool {
    	public void createPool();
    	public Object getUsableObject();
    	public void returnObject(Object obj);
    	public void closeObjectPool();
    }
## 对象池工厂类 ##

    public class ObjectPoolFactory {
	    // 得到对象池的一个单例
	    private static ObjectPoolFactory instance = new ObjectPoolFactory();
	    private ObjectPoolFactory() {
	    }
	    
	    public static ObjectPoolFactory getInstance() {
	    	return instance;
	    }
	    //创建不同类型对象池，设置参数
	    public ObjectPool createPool(Class clsType,int maxNumOfObject, int minNumOfObject, int maxFreeTime){
		    ObjectPool objectPool = new ObjectPool(clsType,maxNumOfObject,minNumOfObject,maxFreeTime);
		    objectPool.createPool();
		    return objectPool;
	    }
	    //关闭对象池
	    public void closePool(ObjectPool objectPool){
	    	objectPool.closeObjectPool();
	    }
    }

## 对象池类 ##

    class ObjectPool implements AbstractPool{
		//初始化对象池为最小值个数。
    	public synchronized void createPool()； 
		//获取对象池中的可用对象，超时抛出异常并返回null。
    	public Object getUsableObject()；
        //归还一个对象到对象池。
		public void returnObject(Object obj)；
		//关闭对象池，同时清空对象池中所有对象。
		public synchronized void closeObjectPool()；
		//清除满足最大空闲时间对象的时间巡检线程类，当对象池未关闭时定时巡检，对象池关闭后线程结束。
		private static class GarbageCollect implements Runnable；
    }


# 使用文档 #
    public class test {
	    public static void main(String[] args) {
	    //创建对象工厂
	    ObjectPoolFactory poolFactory = ObjectPoolFactory.getInstance();
	    //利用对象池工厂,创建一个存放指定类型对象的对象池，并设置最大对象数，最小对象数，最大空闲时间
	    ObjectPool objPool = poolFactory.createPool(ArrayList.class,10,2,1000);

	    // 从对象池中拿出一个可用对象
	    Object obj = objPool.getUsableObject();
	    //打印出对象池当前活跃对象数
	    System.out.println("当前活跃对象数："+objPool.getNumActive());
	    //外部对对象进行操作
	    System.out.println("obj===" + obj.getClass()+" "+obj.toString());
	    //使用完毕后把对象还回对象池
	    objPool.returnObject(obj);
	    //外部对象置位，待GC回收
	    obj=null;
	    
	    Object obj1 = objPool.getUsableObject();
	    System.out.println("当前活跃对象数："+objPool.getNumActive());
	    
	    Object obj2 = objPool.getUsableObject();
	    System.out.println("当前活跃对象数："+objPool.getNumActive());
	    
	    Object obj3 = objPool.getUsableObject();
	    System.out.println("当前活跃对象数："+objPool.getNumActive());
	    objPool.returnObject(obj3);
	    
	    Object obj4 = objPool.getUsableObject();
	    System.out.println("当前活跃对象数："+objPool.getNumActive());

	    //销毁对象池以及所有对象
	    poolFactory.closePool(objPool);
	    }
    }
    