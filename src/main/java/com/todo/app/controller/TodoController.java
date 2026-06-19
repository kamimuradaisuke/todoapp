package com.todo.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.todo.app.entity.Todo;
import com.todo.app.mapper.TodoMapper;


@Controller
public class TodoController {

    private static final Logger logger =
            LoggerFactory.getLogger(TodoController.class);

    @Autowired
    TodoMapper todoMapper;

    @RequestMapping("/")
    public String index(Model model) {

        List<Todo> list = todoMapper.selectAll();

        model.addAttribute("todos", list);
        long doneCount = list.stream()
                .filter(todo -> todo.getDoneFlg() == 1)
                .count();

        model.addAttribute("doneCount", doneCount);

        return "index";
    }

    @RequestMapping("/add")
    public String add(Todo todo) {

        logger.info("Todo追加開始 taskName={}", todo.getTaskName());

        todoMapper.add(todo);

        logger.info("Todo追加完了");

        return "redirect:/detail?taskId="+todo.getTaskId();
    }
   
    
    @RequestMapping("/done")
    public String done(Long taskId) {

        todoMapper.done(taskId);

        return "redirect:/";
    }

    @RequestMapping("/delete")
    @ResponseBody
    public void delete() {

        logger.info("完了済みTodo削除");

        todoMapper.delete();
    }
    
    @PostMapping("/deleteComplete")
    public String deleteComplete() {

        todoMapper.deleteComplete();

        return "redirect:/";
    }
    
    @PostMapping("/undone")
    public String undone(Integer taskId) {

        todoMapper.undone(taskId);

        return "redirect:/";
    }
    
    @RequestMapping("/detail")
    public String detail(Long taskId,Model model) {

        Todo todo = todoMapper.selectById(taskId);

        List<Todo> subTasks =
                todoMapper.selectSubTask(taskId);

        model.addAttribute("todo",todo);
        model.addAttribute("subTasks", subTasks);

        return "detail";
    }
    
    @PostMapping("/update")
    public String update(Todo todo) {
    	todoMapper.update(todo);
    	return "redirect:/detail?taskId=" + todo.getTaskId();
    	
    }
    
    @RequestMapping("/subtask/add")
    public String subtaskAdd(Long parentId, Model model) {

        Todo parent = todoMapper.selectById(parentId);

        model.addAttribute("parent", parent);

        return "subtaskAdd";
    }
    
    @PostMapping("/addSubTask")
    public String addSubTask(Todo todo) {

        Long parentId = todo.getParentId();
        todoMapper.add(todo);
        return "redirect:/detail?taskId=" + parentId;
    }
    @RequestMapping("/subtask/detail")
    public String subtaskDetail(Long taskId, Model model) {

        Todo todo = todoMapper.selectById(taskId);

        Todo parentTask =
                todoMapper.selectById(todo.getParentId());

        model.addAttribute("todo", todo);
        model.addAttribute("parentTask", parentTask);

        return "subtaskDetail";
    }
}
