package com.todo.app.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.todo.app.entity.Todo;
import com.todo.app.mapper.TodoMapper;




@Controller
public class TodoController {

    private static final Logger logger =
            LoggerFactory.getLogger(TodoController.class);
    /** Todo操作用Mapper */
    @Autowired
    TodoMapper todoMapper;
    /**
     * マイタスク一覧画面を表示する
     *
     * 親タスク・サブタスクを取得しツリー構造を作成する。
     * また完了件数を集計して画面に渡す。
     */
    @RequestMapping("/")
    public String index(Model model) {
    	//全タス取得
        List<Todo> list = todoMapper.selectAll();
        
        //親と子の紐付け
        for (Todo parent : list) {
            if (parent.getParentId() == null) {
                List<Todo> children =
                        todoMapper.selectSubTask(parent.getTaskId());
                parent.setSubTasks(children);
            }
        }
        //画面表示用データ
        model.addAttribute("todos", list);
        
        //完了件数計算
        long doneCount = list.stream()
                .filter(todo -> todo.getDoneFlg() == 1)
                .count();
        
        model.addAttribute("doneCount", doneCount);
        
        //追加用の空オブジェクト
        model.addAttribute("todo", new Todo());
        //プルダウン用データ
        model.addAttribute("priorityList",todoMapper.selectPriorityList());
        model.addAttribute("catgoryList",todoMapper.selectCategoryList());
        

        return "index";
    }
    /**
     * 
     * 新規追加
     * 入力された情報を詳細画面へ
     */
    @PostMapping("/add")
    public String add(@Valid Todo todo,
                      BindingResult result) {
    	
    	//エラー時に一覧画面に戻す
        if (result.hasErrors()) {
            return "todoList";
        }
        
        logger.info("Todo追加開始 taskName={}", todo.getTaskName());
        
        //DBに登録
        todoMapper.add(todo);

        logger.info("Todo追加完了");

        return "redirect:/detail?taskId=" + todo.getTaskId();
    }
    
    //タスクを完了状態にする
    @RequestMapping("/done")
    public String done(Long taskId) {
    	
        todoMapper.done(taskId);
        todoMapper.doneSubTask(taskId);

        return "redirect:/";
    }
    //タスクを未完了状態にする
    @PostMapping("/undone")
    public String undone(Integer taskId) {
    	
        todoMapper.undone(taskId);
        todoMapper.undoneSubTask(Long.valueOf(taskId));

        return "redirect:/";
    }
    
    // 完了済みTodoを削除する（内部処理用）
    @RequestMapping("/delete")
    @ResponseBody
    public void delete() {

        logger.info("完了済みTodo削除");

        todoMapper.delete();
    }
    
    //完了済みタスクを一括削除
    @PostMapping("/deleteComplete")
    public String deleteComplete() {

        todoMapper.deleteComplete();

        return "redirect:/";
    }
    
    //詳細画面を表示する
    @RequestMapping("/detail")
    public String detail(Long taskId,Model model) {
    	//親タスク取得
        Todo todo = todoMapper.selectById(taskId);
        //子タスク取得
        List<Todo> subTasks =
                todoMapper.selectSubTask(taskId);
        //画面へデータ設定
        model.addAttribute("todo",todo);
        model.addAttribute("subTasks", subTasks);
        model.addAttribute("priorityList",todoMapper.selectPriorityList());
        model.addAttribute("catgoryList",todoMapper.selectCategoryList());

        return "detail";
    }
    
   /**
    * 親タスクを更新する
    * 親の状態に応じて子の状態も同期更新する
    */
    @PostMapping("/update")
    public String update(Todo todo) {
    	
    	//タスク更新
        todoMapper.update(todo);
        
        //状態の応じて子タスクの更新
        if (todo.getDoneFlg() == 1) {
            todoMapper.doneSubTask(todo.getTaskId());
        } else {
            todoMapper.undoneSubTask(todo.getTaskId());
        }

        return "redirect:/detail?taskId=" + todo.getTaskId();
    }
    
    //サブタスク追加画面を表示する
    @RequestMapping("/subtask/add")
    public String subtaskAdd(Long parentId, Model model) {
    	//親タスク取得
        Todo parent = todoMapper.selectById(parentId);

        model.addAttribute("parent", parent);


        return "subtaskAdd";
    }
    
    //サブタスクを追加する
    @PostMapping("/addSubTask")
    public String addSubTask(Todo todo) {

        todoMapper.add(todo);

        return "redirect:/subtask/detail?taskId=" + todo.getTaskId();
    }
    
    
    //サブタスク詳細画面を表示する
    @RequestMapping("/subtask/detail")
    public String subtaskDetail(Long taskId, Model model) {
    	//サブタスク取得
        Todo todo = todoMapper.selectById(taskId);
        //親タスク取得
        Todo parentTask =
                todoMapper.selectById(todo.getParentId());
        //画面へデータの設定
        model.addAttribute("todo", todo);
        model.addAttribute("parentTask", parentTask);
        model.addAttribute("priorityList",todoMapper.selectPriorityList());
        model.addAttribute("catgoryList",todoMapper.selectCategoryList());

        return "subtaskDetail";
    }
    
    //サブタスクを更新する
    @PostMapping("/updateSubTask")
    public String updateSubTask(Todo todo) {

        
        todoMapper.update(todo);


        return "redirect:/subtask/detail?taskId="
                + todo.getTaskId();
    }
}


