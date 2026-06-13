package ${servicePackage};

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import ${dtoPackage}.${entity}AddRequest;
import ${dtoPackage}.${entity}QueryRequest;
import ${dtoPackage}.${entity}UpdateRequest;
import ${entityPackage}.${entity};
import ${voPackage}.${entity}VO;

/**
 * ${table.comment!entity}服务
 */
public interface ${table.serviceName} extends IService<${entity}> {

    /**
     * 新增
     *
     * @param ${entityLower}AddRequest 新增请求
     * @return 新增记录 id
     */
    long add${entity}(${entity}AddRequest ${entityLower}AddRequest);

    /**
     * 删除
     *
     * @param id 主键 id
     * @return 是否删除成功
     */
    boolean delete${entity}(Long id);

    /**
     * 更新
     *
     * @param ${entityLower}UpdateRequest 更新请求
     * @return 是否更新成功
     */
    boolean update${entity}(${entity}UpdateRequest ${entityLower}UpdateRequest);

    /**
     * 根据 id 获取详情
     *
     * @param id 主键 id
     * @return 视图对象
     */
    ${entity}VO get${entity}VOById(Long id);

    /**
     * 分页查询
     *
     * @param ${entityLower}QueryRequest 查询请求
     * @return 分页结果
     */
    Page<${entity}VO> list${entity}VOByPage(${entity}QueryRequest ${entityLower}QueryRequest);
}
