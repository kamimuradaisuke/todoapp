package com.todo.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

        return "index";
    }

    @RequestMapping("/add")
    public String add(Todo todo) {

        logger.info("Todo追加開始 taskName={}", todo.getTaskName());

        todoMapper.add(todo);

        logger.info("Todo追加完了");

        return "redirect:/";
    }

    @RequestMapping("/update")
    @ResponseBody
    public void update(Todo todo) {

        logger.info("Todo更新 =taskId{}", todo.getTaskId());

        todoMapper.update(todo);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public void delete() {

        logger.info("完了済みTodo削除");

        todoMapper.delete();
    }
}
