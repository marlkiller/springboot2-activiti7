import com.zjialin.workflow.Application;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author voidm
 * @date 2019/12/28
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})// 指定启动类
public class TestDemo {


    /**
     * 1. 根据流程图 部署
     */
    @Test
    public void deploy() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();

        // 进行部署
        Deployment deployment = repositoryService.createDeployment()//创建Deployment对象
                .addClasspathResource("processes/custom_leave.bpmn")//添加bpmn文件
                // .addClasspathResource("diagram/holiday.png")//添加png文件
                .name("请假申请单流程")
                .deploy();//部署

        // 输出部署的一些信息
        System.out.println(deployment.getName()); // 请假申请单流程
        System.out.println(deployment.getId()); // 3e014bc6-2956-11ea-ad22-feaf48b96e42
    }


    /**
     * 2. 部署后获取流程实例
     */
    @Test
    public void start() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<>();
        variables.put("startUserKey", "startUserKey");
        // 创建流程实例(关键步骤)即 启动流程实例
        // 找key的方法  1：bpmn文件中的id，它对应的值就是key 2：直接看数据库中流程定义表act_re_procdet的key值）
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process_id", variables);
        // 输出实例的相关信息
        System.out.println("流程部署ID=" + processInstance.getDeploymentId());//null
        System.out.println("流程定义ID=" + processInstance.getProcessDefinitionId());//process_id:2:6396446b-2956-11ea-8ff9-feaf48b96e42
        System.out.println("流程实例ID=" + processInstance.getId());//d30d9429-2956-11ea-af98-feaf48b96e42
        System.out.println("流程活动ID=" + processInstance.getActivityId());//获取当前具体执行的某一个节点的ID(null)
    }


    /**
     * 根据流程实例获取 task
     * 将 task 移交给某人
     * 做查询
     */
    @Test
    public void query() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();

        // 根据实例 id 查询
        for (Task task : taskService.createTaskQuery().processInstanceId("d30d9429-2956-11ea-af98-feaf48b96e42").list()) {
            System.out.println(task.getId()); // d31164bd-2956-11ea-af98-feaf48b96e42

            // 转让
            taskService.setAssignee(task.getId(), "taskUserKey");
        }
        // 根据 processDefinitionKey 查询
        // 根据流程定义的key以及负责人 assignee 来实现当前用户的任务列表查询
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey("process_id")
                .taskAssignee("taskUserKey")
                .list();//这里还有一个查询唯一结果的方法：singleResult();、还有分页查询listPage(index,limit);
        // 任务列表展示
        for (Task task : taskList) {
            //查的act_hi_procinst表的id
            System.out.println("流程实例ID=" + task.getProcessInstanceId());
            //查的act_hi_taskinst表的id
            System.out.println("任务ID=" + task.getId());
            //查的act_hi_taskinst表的Assignee_
            System.out.println("任务负责人名称=" + task.getAssignee());
            //查的act_hi_taskinst表的NAME_
            System.out.println("任务名称=" + task.getName());
        }
    }


    /**
     * 根据 taskId 完成人物
     */
    @Test
    public void complete() {

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();

        // 处理任务,结合当前用户任务列表的查询操作的话，可以知道任务ID=5002(实际操作中应该与查询写在一起)
        taskService.complete("d31164bd-2956-11ea-af98-feaf48b96e42");
    }
}