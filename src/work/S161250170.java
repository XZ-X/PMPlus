package work;

import bottom.BottomMonitor;
import bottom.BottomService;
import bottom.Task;
import main.Schedule;

import java.io.IOException;

/**
 * 注意：请将此类名改为 S+你的学号   eg: S161250001
 * 提交时只用提交此类和说明文档
 * <p>
 * 在实现过程中不得声明新的存储空间（不得使用new关键字，反射和java集合类）
 * 所有新声明的类变量必须为final类型
 * 不得创建新的辅助类
 * <p>
 * 可以生成局部变量
 * 可以实现新的私有函数
 * <p>
 * 可用接口说明:
 * <p>
 * 获得当前的时间片
 * int getTimeTick()
 * <p>
 * 获得cpu数目
 * int getCpuNumber()
 * <p>
 * 对自由内存的读操作  offset 为索引偏移量， 返回位置为offset中存储的byte值
 * byte readFreeMemory(int offset)
 * <p>
 * 对自由内存的写操作  offset 为索引偏移量， 将x写入位置为offset的内存中
 * void writeFreeMemory(int offset, byte x)
 */
public class S161250170 extends Schedule {
    /**
     * MEMORY STRUCTURE
     * 0-31             variable
     * 32-4*1024        PCB
     * 4*1024-20*1024   GET_RESOURCES
     */

    //目前存放到第几个了
    private final int TASK_LIST_INDEX_ADDR = 0;
    //任务列表的基址
    /**
     * 任务列表格式
     * <p>
     * byte 0-3     任务ID
     * byte 4-7     剩余CPU时间
     * byte 8       资源个数
     * byte 9       valid
     * byte 10-15   空
     * byte 16-31   RESOURCE
     */
    private final int TASK_LIST_ITEM_SIZE = 32;

    private final int GET_ID = 0;
    private final int GET_CPU_TIME = 4;
    private final int GET_RES_CNT = 8;
    private final int IS_VALID = 9;
    //    private final int FIRST_RESOURCE = 10;
//    private final int SECOND_RESOURCE = 11;
    private final int GET_RESOURCES = 16;

    /**
     * 4*4 = 16byte = 128 位 bitmap
     */
    private final int BIT_MAP_ADDR = 8;

    //当前处理到的任务
    private final int CURRENT_TASK = 8 + 16;
    //当前处理到第几个了
    private final int TASK_PROCESS_INDEX_ADDR = 28;

    private final int TASK_LIST_BASE_ADDR = 32;

    private final int TASK_RESOURCE_BASE_ADDR = 20 * 1024;

    private final int PCB_LIMIT = TASK_RESOURCE_BASE_ADDR - TASK_LIST_BASE_ADDR;

    private final int TASK_RESOURCE_LIMIT = 0;
    //任务资源表的当前空闲地址
    private final int CURRENT_SPACE = 4;


    @Override
    public void ProcessSchedule(Task[] arrivedTask, int[] cpuOperate) {
        int timing = getTimeTick();
        int cpu = 0;
        //储存新到达的任务
        for (Task task : arrivedTask) {
            storeTask(task);
        }
        int currentTaskIndex = readInt(TASK_PROCESS_INDEX_ADDR);
        int waitingListLength = readInt(TASK_LIST_INDEX_ADDR);
        releaseAll();
        //refresh task state
        while (currentTaskIndex < waitingListLength && readInt(lea(currentTaskIndex) + GET_CPU_TIME) <= 0) {
            writeFreeMemory(lea(currentTaskIndex) + IS_VALID, ((byte) 0));
            currentTaskIndex++;
        }
        storeInt(TASK_PROCESS_INDEX_ADDR, currentTaskIndex);

        int localIndex = currentTaskIndex;
        //当还有剩余任务&&CPU时间还有空余
        while (localIndex < waitingListLength && cpu < getCpuNumber()) {
            if (readFreeMemory(lea(localIndex) + IS_VALID) == 1) {
                //任务还未被处理
                if (readInt(lea(localIndex) + GET_CPU_TIME) > 0) {
                    int taskID = readInt(lea(localIndex) + GET_ID);
                    //任务资源可获取
                    if (touchResource(lea(localIndex) + GET_RESOURCES)) {

                        int time = readInt(lea(localIndex) + GET_CPU_TIME);
                        cpuOperate[cpu++] = readInt(lea(localIndex) + GET_ID);
                        time--;
                        storeInt(lea(localIndex) + GET_CPU_TIME, time);
                    }
                }
            }
            localIndex++;
        }

//        //新到达的任务
//        for (Task task : arrivedTask) {
//            if (cpu < getCpuNumber()) {
//                if (touchResource(task)) {
//                    cpuOperate[cpu++] = task.tid;
//                    task.cpuTime--;
//                }
//            }
//            storeTask(task);
//        }

//        int currentTaskAddr = readInt(CURRENT_TASK);

    }

    /**
     * 加载有效任务地址
     */
    private int lea(int index) {
        return TASK_LIST_BASE_ADDR + ((index * TASK_LIST_ITEM_SIZE) % PCB_LIMIT);
    }

//    private boolean touchResource(Task task) {
//        boolean ret = true;
//        for (int resource : task.resource) {
//            if (!useResource(resource)) {
//                ret = false;
//                break;
//            }
//        }
//        releaseAll(task.resource);
//        return ret;
//    }

    private boolean touchResource(int baseAddr) {
        return useResource(baseAddr);
    }

//    private void releaseAll(int[] resource) {
//        for (int res : resource) {
//            releaseResource(res);
//        }
//    }

    private void releaseAll() {
        storeInt(BIT_MAP_ADDR, 0);
        storeInt(BIT_MAP_ADDR + 4, 0);
        storeInt(BIT_MAP_ADDR + 8, 0);
        storeInt(BIT_MAP_ADDR + 12, 0);
    }

//    private void releaseAll(int base, int number) {
//        for (int i = 0; i < number; i++) {
//            releaseResource(readFreeMemory(base + i));
//        }
//    }
//
//    private boolean releaseResource(int i) {
//        if (i < 32) {
//            int bitmap = readInt(BIT_MAP_ADDR);
//            int mask = ~(1 << i);
//            bitmap &= mask;
//            storeInt(BIT_MAP_ADDR, bitmap);
//            return true;
//        }
//        if (i <= 64) {
//            i -= 32;
//            int bitmap = readInt(BIT_MAP_ADDR + 4);
//            int mask = ~(1 << i);
//            bitmap &= mask;
//            storeInt(BIT_MAP_ADDR + 4, bitmap);
//            return true;
//        }
//        if (i <= 96) {
//            i -= 64;
//            int bitmap = readInt(BIT_MAP_ADDR + 8);
//            int mask = ~(1 << i);
//            bitmap &= mask;
//            storeInt(BIT_MAP_ADDR + 8, bitmap);
//            return true;
//        } else {
//            i -= 96;
//            int bitmap = readInt(BIT_MAP_ADDR + 12);
//            int mask = ~(1 << i);
//            bitmap &= mask;
//            storeInt(BIT_MAP_ADDR + 12, bitmap);
//            return true;
//        }
//    }

    private boolean useResource(int addr) {
        int a1 = readInt(addr);
        int a2 = readInt(addr + 4);
        int a3 = readInt(addr + 8);
        int a4 = readInt(addr + 12);
        boolean b1 = true;
        boolean b2 = true;
        boolean b3 = true;
        boolean b4 = true;
        int map1 = readInt(BIT_MAP_ADDR);
        int map2 = readInt(BIT_MAP_ADDR + 4);
        int map3 = readInt(BIT_MAP_ADDR + 8);
        int map4 = readInt(BIT_MAP_ADDR + 12);

        b1 = (a1 & map1) == 0;
        b2 = (a2 & map2) == 0;
        b3 = (a3 & map3) == 0;
        b4 = (a4 & map4) == 0;
        if (b1 && b2 && b3 && b4) {
            map1 |= a1;
            map2 |= a2;
            map3 |= a3;
            map4 |= a4;
            storeInt(BIT_MAP_ADDR, map1);
            storeInt(BIT_MAP_ADDR + 4, map2);
            storeInt(BIT_MAP_ADDR + 8, map3);
            storeInt(BIT_MAP_ADDR + 12, map4);
            return true;
        } else {
            return false;
        }
//        if (i < 32) {
//            int bitmap = readInt(BIT_MAP_ADDR);
//            int mask = 1 << i;
//            if ((bitmap & mask) != 0) {
//                return false;
//            }
//            bitmap |= mask;
//            storeInt(BIT_MAP_ADDR, bitmap);
//            return true;
//        }
//        if (i <= 64) {
//            i -= 32;
//            int bitmap = readInt(BIT_MAP_ADDR + 4);
//            int mask = 1 << i;
//            if ((bitmap & mask) != 0) {
//                return false;
//            }
//            bitmap |= mask;
//            storeInt(BIT_MAP_ADDR + 4, bitmap);
//            return true;
//        }
//        if (i <= 96) {
//            i -= 64;
//            int bitmap = readInt(BIT_MAP_ADDR + 8);
//            int mask = 1 << i;
//            if ((bitmap & mask) != 0) {
//                return false;
//            }
//            bitmap |= mask;
//            storeInt(BIT_MAP_ADDR + 8, bitmap);
//            return true;
//        } else {
//            i -= 96;
//            int bitmap = readInt(BIT_MAP_ADDR + 12);
//            int mask = 1 << i;
//            if ((bitmap & mask) != 0) {
//                return false;
//            }
//            bitmap |= mask;
//            storeInt(BIT_MAP_ADDR + 12, bitmap);
//            return true;
//        }
    }

    private void storeTask(Task task) {
        int index = readInt(TASK_LIST_INDEX_ADDR);

        int addr = lea(index);
        storeInt(addr + GET_ID, task.tid);

        storeInt(addr + GET_CPU_TIME, task.cpuTime);

        writeFreeMemory(addr + GET_RES_CNT, ((byte) task.resource.length));
        //valid
        writeFreeMemory(addr + IS_VALID, ((byte) 1));
//        if (task.resource.length > 0) {
//            writeFreeMemory(addr, ((byte) task.resource[0]));
//        }
//        if (task.resource.length > 1) {
//            writeFreeMemory(addr, ((byte) task.resource[1]));
//        }
//        if (task.resource.length > 2) {
        storeResource(addr + GET_RESOURCES, task.resource);
//        storeInt(addr + GET_RESOURCES, resource);
//        }
        index++;
        storeInt(TASK_LIST_INDEX_ADDR, index);

    }

//    private int loadResAddr(int addr) {
//        if (addr == 0) {
//            //init
//            return TASK_RESOURCE_BASE_ADDR;
//        } else {
//            addr -= TASK_RESOURCE_BASE_ADDR;
//            addr %= TASK_RESOURCE_LIMIT;
//            return addr + TASK_RESOURCE_BASE_ADDR;
//        }
//    }

    /**
     * 储存所需资源
     */
    private void storeResource(int addr, int[] resources) {
        int a1 = 0;
        int a2 = 0;
        int a3 = 0;
        int a4 = 0;
        for (int resource : resources) {
            switch (resource / 32) {
                case 0:
                    a1 |= 1 << resource;
                    break;
                case 1:
                    a2 |= 1 << (resource - 32);
                    break;
                case 2:
                    a3 |= 1 << (resource - 64);
                    break;
                case 3:
                    a4 |= 1 << (resource - 96);
                    break;
            }
        }
        storeInt(addr, a1);
        storeInt(addr + 4, a2);
        storeInt(addr + 8, a3);
        storeInt(addr + 12, a4);
//        if (resources.length <= 2) {
//            return 0;
//        } else {
//        int index = loadResAddr(readInt(CURRENT_SPACE));
//        int baseAddr = loadResAddr(index);
//        for (int i = 0; i < resources.length; i++) {
//            writeFreeMemory(loadResAddr(index), ((byte) resources[i]));
//            index++;
//        }
//        index--;
//        storeInt(CURRENT_SPACE, index);
//        return baseAddr;
//        }
    }


    private void storeInt(int addr, int content) {
        writeFreeMemory(addr++, ((byte) (content & 0xff)));
        writeFreeMemory(addr++, ((byte) ((content & 0xff00) >> 8)));
        writeFreeMemory(addr++, ((byte) ((content & 0xff0000) >> 16)));
        writeFreeMemory(addr, ((byte) ((content & 0xff000000) >> 24)));
    }


    private int readInt(int addr) {
        int b0 = readFreeMemory(addr++);
        int b1 = readFreeMemory(addr++);
        int b2 = readFreeMemory(addr++);
        int b3 = readFreeMemory(addr);
        b0 &= 0xff;
        b1 &= 0xff;
        b2 &= 0xff;
        b3 &= 0xff;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    /**
     * 执行主函数 用于debug
     * 里面的内容可随意修改
     * 你可以在这里进行对自己的策略进行测试，如果不喜欢这种测试方式，可以直接删除main函数
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // 定义cpu的数量
        int cpuNumber = 2;
        // 定义测试文件
        String filename = "src/testFile/textSample.txt";

        BottomMonitor bottomMonitor = new BottomMonitor(filename, cpuNumber);
        BottomService bottomService = new BottomService(bottomMonitor);
        Schedule schedule = new S161250170();
        schedule.setBottomService(bottomService);

        //外部调用实现类
        for (int i = 0; i < 5000; i++) {
            Task[] tasks = bottomMonitor.getTaskArrived();
            int[] cpuOperate = new int[cpuNumber];

            // 结果返回给cpuOperate
            schedule.ProcessSchedule(tasks, cpuOperate);

            try {
                bottomService.runCpu(cpuOperate);
            } catch (Exception e) {
                System.out.println("Fail: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            bottomMonitor.increment();
        }

        //打印统计结果
        bottomMonitor.printStatistics();
        System.out.println();

        //打印任务队列
        bottomMonitor.printTaskArrayLog();
        System.out.println();

        //打印cpu日志
        bottomMonitor.printCpuLog();


        if (!bottomMonitor.isAllTaskFinish()) {
            System.out.println(" Fail: At least one task has not been completed! ");
        }
    }

}
