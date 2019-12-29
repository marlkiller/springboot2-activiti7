import com.zjialin.workflow.Application;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
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
     * 影响的activiti表有哪些
     * act_re_deployment 部署信息
     * act_re_procdef    流程定义的一些信息
     * act_ge_bytearray  流程定义的bpmn文件以及png文件
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
        System.out.println(deployment.getId()); // 327b7c86-29e3-11ea-8893-064a7eedaca9
    }


    /**
     * 2. 部署后获取流程实例
     * 前提是先已经完成流程定义的部署工作
     * 背后影响的表：
     * act_hi_actinst      已完成的活动信息
     * act_hi_identitylink   参与者信息
     * act_hi_procinst     流程实例
     * act_hi_taskinst     任务实例
     * act_ru_execution    执行表
     * act_ru_identitylink   参与者信息
     * act_ru_task   任务表
     */
    @Test
    public void start() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<>();
        variables.put("startUserKey", "startUserKey");
        // 创建流程实例(关键步骤)即 启动流程实例
        // 找key的方法  1：bpmn文件中的id，它对应的值就是key 2：直接看数据库中流程定义表act_re_procdet的key值）
        // ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process_id", "businessKey", variables);
        ProcessDefinition processDefinition =
                processEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId("327b7c86-29e3-11ea-8893-064a7eedaca9").singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId(), "businessKey", variables);
        // 输出实例的相关信息
        System.out.println("流程部署ID=" + processInstance.getDeploymentId());//null
        System.out.println("流程定义ID=" + processInstance.getProcessDefinitionId());//process_id:1:32960968-29e3-11ea-8893-064a7eedaca9
        System.out.println("流程实例ID=" + processInstance.getId());// b4808dd0-29e3-11ea-8279-064a7eedaca9
        System.out.println("流程活动ID=" + processInstance.getActivityId());//获取当前具体执行的某一个节点的ID(null)
    }


    /**
     * 根据流程实例获取 task
     * 将 task 移交给某人
     * 做查询
     * 处理当前用户的任务列表
     * 背后操作到的表：
     * act_hi_actinst
     * act_hi_identitylink
     * act_hi_taskinst
     * act_ru_execution
     * act_ru_identitylink
     * act_ru_task //只放当前要执行的任务
     */
    @Test
    public void submitLeaveRequest() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();

        // 根据实例 id 查询
        for (Task task : taskService.createTaskQuery()
                .processInstanceId("b4808dd0-29e3-11ea-8279-064a7eedaca9").list()) {
            System.out.println(task.getId()); // d31164bd-2956-11ea-af98-feaf48b96e42

            // 转让, 这里正式开始 提交请假单
            // taskService.setAssignee(task.getId(), "taskUserKey");
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", "voidm");
            map.put("age", 19);
            System.out.println(map);
            // 提交成功后, 触发器会将 Assignee 设置为 fuck
            taskService.complete(task.getId(), map);
        }
    }

    @Test
    public void approval() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        // 根据 processDefinitionKey 查询
        // 根据流程定义的key以及负责人 assignee 来实现当前用户的任务列表查询
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey("process_id")
                .processInstanceId("b4808dd0-29e3-11ea-8279-064a7eedaca9")
                .processInstanceBusinessKey("businessKey")
                .taskAssignee("fuck")
                .includeProcessVariables()
                .list();//这里还有一个查询唯一结果的方法：singleResult();、还有分页查询listPage(index,limit);
        // 任务列表展示
        for (Task task : taskList) {
            System.out.println("task.getProcessDefinitionId() = " + task.getProcessDefinitionId()); // process_id:1:32960968-29e3-11ea-8893-064a7eedaca9
            //查的act_hi_procinst表的id
            System.out.println("流程实例ID=" + task.getProcessInstanceId());
            //查的act_hi_taskinst表的id
            System.out.println("任务ID=" + task.getId());
            //查的act_hi_taskinst表的Assignee_
            System.out.println("任务负责人名称=" + task.getAssignee());
            //查的act_hi_taskinst表的NAME_
            System.out.println("任务名称=" + task.getName());
            System.out.println(task.getProcessVariables());

            // 任务完成
            taskService.complete(task.getId(), task.getProcessVariables());
            System.out.println("人物完成：已经提交");

            taskList = taskService.createTaskQuery()
                    .processDefinitionKey("process_id")
                    .taskAssignee("fuck")
                    .includeProcessVariables()
                    .list();
            System.out.println("taskList.size() = " + taskList.size());
        }
    }
}