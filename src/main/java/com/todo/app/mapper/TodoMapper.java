package com.todo.app.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.todo.app.entity.Category;
import com.todo.app.entity.Priority;
import com.todo.app.entity.Todo;


@Mapper
public interface TodoMapper {

	public List<Todo> selectAll();
	public List<Todo> selectIncomplete();
	public List<Todo> selectComplete();
	public List<Priority> selectPriorityList();
	public List<Category> selectCategoryList();
	public List<Todo> selectSubTask(Long parentId);
	
	public void add(Todo todo);
	public void update(Todo todo);
	public void delete();
	public Todo selectById(Long taskId);
	public void done(Long taskId);
	public void doneSubTask(Long taskId);
	public void undone(Integer taskId);
	public void undoneSubTask(Long taskId);
	public void deleteComplete();	
	
	public List<Todo>search(
			@Param("taskName") String taskName,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("doneFlg") Integer doneFlg,
			@Param("priorityId") Integer priorityId,
			@Param("categoryId") Integer categoryId
			);
}

