package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品數據
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 更新菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish selectById(Long id);

    /**
     * 批量删除菜品
     * 根據主鍵刪除菜品數據
     * @param id
     */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    /**
     *根据分类id查询菜品
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);

    /**
     * 根据套餐id查询关联的菜品id
     * @param setmealId
     * @return
     */
    @Select("select d.* from dish d left join setmeal_dish sd on d.id = sd.dish_id where setmeal_id = #{setmealId}")
    List<Dish> getDishIdBySetmealId(Long setmealId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 根據菜品id集合批量刪除菜品
     * @param ids
     */
    void deleteByIds(List<Long> ids);




}
