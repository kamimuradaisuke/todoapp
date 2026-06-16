package com.todo.app.entity;

import lombok.Data;

@Data
public class Todo {

	private Long taskId;			// タスクID
	private String taskName;		//タスク名
	private Integer priorityId;		//優先度
	private Integer categoryId;		//カテゴリ
	private String day;				//実施予定日
	private String memo;   			//メモ
}
