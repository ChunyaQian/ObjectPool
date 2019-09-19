package pool;

import java.lang.reflect.Array;
import java.util.ArrayList;

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
