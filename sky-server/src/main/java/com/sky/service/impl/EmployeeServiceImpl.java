package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //密码进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增員工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        System.out.println("當前線程的id:" + Thread.currentThread().getId());
        Employee employee = new Employee();

        //對象屬性拷貝
        BeanUtils.copyProperties(employeeDTO,employee);

        //設置帳號狀態,默認正常狀態 1表示正常 0表示鎖定
        employee.setStatus(StatusConstant.ENABLE);

        //設置密碼,默認密碼為123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //設置當前紀錄的創建時間和修改時間
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //設置當前紀錄創建人id和修改人id
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */

    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //select * from employee limit page,pageSize,使用PageHelper插件可以实现limit关键字的自动拼接以及自动查询

        //使用PageHelper的startPage方法设置好分页参数
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        //接收查询返回的Page集合,在调用Page提供的方法时可以实现自动查询(mapper层不用写查询语句)
        Page<Employee> p = employeeMapper.pageQuery(employeePageQueryDTO);
        //使用Page集合提供的方法获取查询的总记录数和当前页数的数据集合
        long total = p.getTotal();
        List<Employee> records = p.getResult();
        for (Employee employee : records) {
            employee.setPassword("****");
        }
        PageResult pageResult = new PageResult(total, records);

        return pageResult;
    }

    /**
     * 启用禁用员工账号
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //this.authentication();
        //update employee set status = status where id = id
        //这里把传进来的参数封装到对象里
//        Employee employee = new Employee();
//        employee.setId(id);
//        employee.setStatus(status);
        //使用构建器builder来创建新对象的同时给对象属性赋值
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        //直接调用update方法传入对象,提高代码的复用性
        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工
     *
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.select(id);
        employee.setPassword("****");
        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        //this.authentication();
        Employee employee = new Employee();
        //把employeeDTO中的属性拷贝到employee中
        BeanUtils.copyProperties(employeeDTO,employee);
        //使用aop + 反射为公共字段自动填充
        //重置修改时间和修改人
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     */

    public void editPassword(PasswordEditDTO passwordEditDTO) {
        Employee employee = employeeMapper.select(BaseContext.getCurrentId());
        String oldPassword = DigestUtils.md5DigestAsHex(passwordEditDTO.getOldPassword().getBytes());
        //进行密码比对
        if (!employee.getPassword().equals(oldPassword)) {
            //输入的原始密码与当前用户密码不符，抛出密码不一致异常
            //throw new PasswordErrorException(PasswordConstant.PASSWORD_INCONSISTENT);
        }
        //新密码使用md5加密后更新员工信息
        employee.setPassword(DigestUtils.md5DigestAsHex(passwordEditDTO.getNewPassword().getBytes()));
        employeeMapper.update(employee);

    }

    /**
     * 判断当前账号是否为管理员账号
     * 非管理员账号无法对员工信息进行增删改功能
     */
    /*public void authentication() {
        if (BaseContext.getCurrentId() != 1) {
            throw new PermissionException(MessageConstant.INSUFFICIENT_PERMISSIONS);
        }
    }*/

}
