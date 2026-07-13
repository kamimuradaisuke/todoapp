package com.todo.app.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.todo.app.entity.FileInfo;
import com.todo.app.entity.Todo;
import com.todo.app.mapper.FileMapper;
import com.todo.app.mapper.TodoMapper;


@Controller
public class TodoController {

    private static final Logger logger =
            LoggerFactory.getLogger(TodoController.class);
    private final String uploadDir = "uploads";
    /** Todo操作用Mapper */
    @Autowired
    TodoMapper todoMapper;
    @Autowired
    FileMapper fileMapper;
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
     * 新規Todoを追加する
     *
     * @param todo 入力されたTodo情報
     * @return 詳細画面へリダイレクト
     */
    @PostMapping("/add")
    public String add(
    		@Valid Todo todo,
            BindingResult result,
            Model model,
            @RequestParam("files") MultipartFile[] files)
    		throws IOException{
    	
    	//エラー時に一覧画面に戻す
        if (result.hasErrors()) {
        	System.out.println(result.getAllErrors());
            return index(model);
        }
        //Todo登録
        todoMapper.add(todo);
        
        //添付ファイル登録
        for (MultipartFile file : files) {
        	if (file.isEmpty()) {
        		continue;
        	}
        	String saveName = file.getOriginalFilename();
        	Path path = Paths.get(uploadDir, saveName);
        	Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
        	
        	FileInfo info = new FileInfo();
        	info.setTaskId(todo.getTaskId());
        	info.setFileName(saveName);
        	info.setFilePath(path.toString());
        	info.setFileSize(file.getSize());
        	fileMapper.insertFile(info);
        }
        System.out.println("files = " + files.length);
        return "redirect:/detail?taskId=" + todo.getTaskId();
    }
    
    /**
     * タスクを完了状態にする
     * 
     * @param taskId 対象タスクID
     */
    @RequestMapping("/done")
    public String done(Long taskId) {
    	
        todoMapper.done(taskId);
        todoMapper.doneSubTask(taskId);

        return "redirect:/";
    }
    /**
     *タスクを未完了状態にする
     * 
     * @param taskId 対象タスクID
     * @return index画面へリダイレクト
    */
    @PostMapping("/undone")
    public String undone(Integer taskId) {
    	
        todoMapper.undone(taskId);
        todoMapper.undoneSubTask(Long.valueOf(taskId));

        return "redirect:/";
    }
    
    // 完了済みタスク削除する（内部処理用）
    @RequestMapping("/delete")
    @ResponseBody
    public void delete() {

        logger.info("完了済みTodo削除");

        todoMapper.delete();
    }
    
    /**
     *完了済みタスクを一括削除
     * 
     * @return index画面へリダイレクト
     */
    @PostMapping("/deleteComplete")
    public String deleteComplete() {

        todoMapper.deleteComplete();

        return "redirect:/";
    }
    
    /**
     * 詳細画面を表示する
     * @param taskId タスクID
     * @param model 画面表示用モデル
     * @return detail画面
     */
    @RequestMapping("/detail")
    public String detail(Long taskId,Model model) {
    	//親タスク取得
        Todo todo = todoMapper.selectById(taskId);
        //子タスク取得
        List<Todo> subTasks =
                todoMapper.selectSubTask(taskId);
        List<FileInfo> fileList =
                fileMapper.selectByTaskId(taskId);
        //画面へデータ設定
        model.addAttribute("todo",todo);
        model.addAttribute("subTasks", subTasks);
        model.addAttribute("fileList",fileList);
        model.addAttribute("priorityList",todoMapper.selectPriorityList());
        model.addAttribute("catgoryList",todoMapper.selectCategoryList());

        return "detail";
    }
    
   /**
    * 親タスクを更新する
    * 親の状態に応じて子の状態も同期更新する
    * 
    * @param todo 更新対象データ
    * @return detail画面へリダイレクト
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
    
    /**
     * サブタスク追加画面を表示する
     * @param parentId
     * @param model
     * @return
     */
    @RequestMapping("/subtask/add")
    public String subtaskAdd(Long parentId, Model model) {
    	//親タスク取得
        Todo parent = todoMapper.selectById(parentId);

        model.addAttribute("parent", parent);


        return "subtaskAdd";
    }
    
    /**
     * サブタスクを追加する
     * 
     * @param todo 登録するサブタスク
     * @return subtaskDetail画面へリダイレクト
     */
    @PostMapping("/addSubTask")
    public String addSubTask(
            Todo todo,
            @RequestParam("files") MultipartFile[] files)
            throws IOException {

        todoMapper.add(todo);

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                continue;
            }

            String saveName = file.getOriginalFilename();
            Path path = Paths.get(uploadDir, saveName);

            Files.copy(file.getInputStream(),
                    path,
                    StandardCopyOption.REPLACE_EXISTING);

            FileInfo info = new FileInfo();
            info.setTaskId(todo.getTaskId());
            info.setFileName(saveName);
            info.setFilePath(path.toString());
            info.setFileSize(file.getSize());

            fileMapper.insertFile(info);
        }

        return "redirect:/subtask/detail?taskId=" + todo.getTaskId();
    }
    
    /**
     * サブタスク詳細画面を表示する
     * @param taskId サブタスクID
     * @param model 画面表示用モデル
     * @return subutasukDetail画面
     */
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
    
    /**
     * サブタスクを更新する
     * @param todo 更新対象サブタスク
     * @return subtaskDetail画面へリダイレクト
     */
    @PostMapping("/updateSubTask")
    public String updateSubTask(Todo todo) {

        
        todoMapper.update(todo);


        return "redirect:/subtask/detail?taskId="
                + todo.getTaskId();
    }
    
    @PostMapping("/upload")
    public String upload(
    		@RequestParam Long taskId,
    		@RequestParam("files") MultipartFile[] files) throws IOException{
    	
    	for (MultipartFile file : files) {
    		if (file.isEmpty()) {
    			continue;
    		}
    		String saveName = file.getOriginalFilename();
    		Path path = Paths.get(uploadDir,saveName);
    		Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
    		FileInfo info = new FileInfo();
    		info.setTaskId(taskId);
    		info.setFileName(saveName);
    		info.setFilePath(path.toString());
    		info.setContentType(file.getContentType());
    		
    		fileMapper.insertFile(info);
    		}
    	return "redirect:/detail?taskId=" + taskId;
    }
    @RequestMapping("/download")
    public ResponseEntity<InputStreamResource> download(Long fileId) throws IOException {

        FileInfo file = fileMapper.selectByFileId(fileId);

        Path path = Paths.get(file.getFilePath());

        InputStreamResource resource =
                new InputStreamResource(Files.newInputStream(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .contentLength(Files.size(path))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}


