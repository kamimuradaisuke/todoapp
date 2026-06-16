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

        logger.info("Todo一覧画面表示開始");

        List<Todo> list = todoMapper.selectIncomplete();
        List<Todo> doneList = todoMapper.selectComplete();

        logger.debug("未完了件数={}", list.size());
        logger.debug("完了件数={}", doneList.size());

        model.addAttribute("todos", list);
        model.addAttribute("doneTodos", doneList);

        return "index";
    }

    @RequestMapping("/add")
    @ResponseBody
    public Todo add(Todo todo) {

        logger.info("Todo追加開始 title={}", todo.getTitle());

        todoMapper.add(todo);

        logger.info("Todo追加完了 id={}", todo.getId());

        return todo;
    }

    @RequestMapping("/update")
    @ResponseBody
    public void update(Todo todo) {

        logger.info("Todo更新 id={}", todo.getId());

        todoMapper.update(todo);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public void delete() {

        logger.info("完了済みTodo削除");

        todoMapper.delete();
    }
}
