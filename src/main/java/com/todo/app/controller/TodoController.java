package com.todo.app.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.GetMapping;
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
     * タスク登録後に添付ファイルを保存する
     *
     * @param todo 入力されたTodo情報
     * @param result バリデーション結果
     * @param model 画面表示用データ
     * @param file 添付ファイル
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
        	//ファイルが選択されてない場合はスキップ
        	if (file.isEmpty()) {
        		continue;
        	}
        	//ファイル名取得
        	String saveName = file.getOriginalFilename();
        	
        	//保存先パス作成
        	Path path = Paths.get(uploadDir, saveName);
        	
        	//ファイルをuploadsフォルダに保存
        	Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
        	
        	//ファイル情報をDBに登録
        	FileInfo info = new FileInfo();
        	info.setTaskId(todo.getTaskId());
        	info.setFileName(saveName);
        	info.setFilePath(path.toString());
        	info.setFileSize(file.getSize());
        	
        	//ファイル形式取得
        	String contentType = file.getContentType();
        	
        	// 形式が取得できない場合はデフォルト設定
        	if (contentType == null) {
        		contentType = "application/octet-stream";
        	}
        	
        	info.setContentType(file.getContentType());
        	fileMapper.insertFile(info);
        }
        System.out.println("files = " + files.length);
        return "redirect:/detail?taskId=" + todo.getTaskId();
    }
    
    /**
     * タスクを完了状態にする
     * 親タスク完了時は子タスクも完了状態へ変更する
     * 
     * @param taskId 対象タスクID
     * @return index画面へリダイレクト
     */
    @RequestMapping("/done")
    public String done(Long taskId) {
    	
    	//親子タスクを完了状態に変更
        todoMapper.done(taskId);
        //子タスクを完了済状態に変更
        todoMapper.doneSubTask(taskId);

        return "redirect:/";
    }
    /**
     *タスクを未完了状態にする
     *親タスク未完了時は子タスクも未完了状態へ変更する
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
     * 添付ファイル情報も削除する
     * 
     * @return index画面へリダイレクト
     */
    @PostMapping("/deleteComplete")
    public String deleteComplete() {
    	// 完了済みタスクのファイル削除
        fileMapper.deleteCompleteFiles();

        // 完了済みタスク削除
        todoMapper.deleteComplete();

        return "redirect:/";
    }
    
    /**
     * 詳細画面を表示する
     * 親タスク、子タスク、添付ファイル一覧を取得する
     * 
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
        //添付ファイル取得
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
    * 添付ファイルを追加保存する
    * 親の状態に応じて子の状態も同期更新する
    * 
    * @param todo 更新対象データ
    * @param file 添付ファイルを追加保存する
    * @return detail画面へリダイレクト
    */
    @PostMapping("/update")
    public String update(Todo todo,
    		@RequestParam("files") MultipartFile[] files)
            throws IOException{
    	
    	//タスク更新
        todoMapper.update(todo);
        
        //添付ファイルを更新
        for (MultipartFile file : files) {
        	
        	//未選択ファイルは処理しない
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

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            info.setContentType(contentType);

            fileMapper.insertFile(info);
        }
        
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
     * @param files 添付ファイル
     */
    @PostMapping("/addSubTask")
    public String addSubTask(
            Todo todo,
            @RequestParam("files") MultipartFile[] files)
            throws IOException {
    	// サブタスクをDBへ登録
        todoMapper.add(todo);
        
        //添付ファイルを1件ずつ保存
        for (MultipartFile file : files) {
        	
        	//ファイルが添付されていない場合は飛ばす
            if (file.isEmpty()) {
                continue;
            }
            //ファイル名取得
            String saveName = file.getOriginalFilename();
            
            //保存先パスを作成
            Path path = Paths.get(uploadDir, saveName);
            
            //ファイルをuploadsフォルダに保存
            Files.copy(file.getInputStream(),
                    path,
                    StandardCopyOption.REPLACE_EXISTING);
            
            //ファイル情報をDB登録用Entityへ設定
            FileInfo info = new FileInfo();
            info.setTaskId(todo.getTaskId());
            info.setFileName(saveName);
            info.setFilePath(path.toString());
            info.setFileSize(file.getSize());
            
            //Content-Type取得
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            info.setContentType(contentType);
            
            //ファイル情報をDBへ登録
            fileMapper.insertFile(info);
        }

        return "redirect:/subtask/detail?taskId=" + todo.getTaskId();
    }
    
    /**
     * サブタスク詳細画面を表示する
     * サブタスク情報、親タスク情報、添付ファイル一覧を取得する
     * 
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
        List<FileInfo>fileList = 
        		fileMapper.selectByTaskId(taskId);
        //画面へデータの設定
        model.addAttribute("todo", todo);
        model.addAttribute("parentTask", parentTask);
        model.addAttribute("fileList",fileList);
        //プルダウン表示用データの設定
        model.addAttribute("priorityList",todoMapper.selectPriorityList());
        model.addAttribute("catgoryList",todoMapper.selectCategoryList());
        

        return "subtaskDetail";
    }
    
    /**
     * サブタスクを更新する
     * タスク内容を更新し、新しく追加された添付ファイルを保存する
 *
     * @param todo 更新対象サブタスク
     * @param files 追加する添付ファイル
     * @return subtaskDetail画面へリダイレクト
     */
    @PostMapping("/updateSubTask")
    public String updateSubTask(Todo todo,
    		@RequestParam("files") MultipartFile[] files)
    		        throws IOException {
        
        todoMapper.update(todo);
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

            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            info.setContentType(contentType);

            fileMapper.insertFile(info);
        }


        return "redirect:/subtask/detail?taskId="
                + todo.getTaskId();
    }
    
    /**
     * 添付ファイル追加登録処理
     * 既存タスクへファイルのみ追加する
     *
     * @param taskId ファイルを紐づけるタスクID
     * @param files 登録する添付ファイル
     * @return タスク詳細画面へリダイレクト
     */
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
    
    /**
     * 添付ファイルダウンロード処理
     * 指定されたファイルIDからファイル情報を取得し、
     * 保存先のファイルをレスポンスとして返却する
     *
     * @param fileId ダウンロード対象ファイルID
     * @return ファイルデータ
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(
            @RequestParam("fileId") Long fileId) throws IOException {

        FileInfo file = fileMapper.selectByFileId(fileId);

        Path path = Paths.get(file.getFilePath());

        InputStreamResource resource =
                new InputStreamResource(Files.newInputStream(path));

        String encodedFileName =
                URLEncoder.encode(
                        file.getFileName(),
                        StandardCharsets.UTF_8
                );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .contentLength(Files.size(path))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}


