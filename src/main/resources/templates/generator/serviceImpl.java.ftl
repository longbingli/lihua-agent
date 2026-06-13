package ${serviceImplPackage};

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import ${basePackage}.common.ErrorCode;
import ${basePackage}.convert.${entity}Convert;
import ${basePackage}.exception.BusinessException;
import ${basePackage}.exception.ThrowUtils;
import ${dtoPackage}.${entity}AddRequest;
import ${dtoPackage}.${entity}QueryRequest;
import ${dtoPackage}.${entity}UpdateRequest;
import ${mapperPackage}.${table.mapperName};
import ${entityPackage}.${entity};
import ${servicePackage}.${table.serviceName};
import ${voPackage}.${entity}VO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ${table.comment!entity}服务实现
 */
@Service
public class ${table.serviceImplName} extends ServiceImpl<${table.mapperName}, ${entity}>
        implements ${table.serviceName} {

    @Override
    public long add${entity}(${entity}AddRequest ${entityLower}AddRequest) {
        ThrowUtils.throwIf(${entityLower}AddRequest == null, ErrorCode.PARAMS_ERROR);
        ${entity} ${entityLower} = ${entity}Convert.INSTANCE.toEntity(${entityLower}AddRequest);
        valid${entity}(${entityLower}, true);
        boolean result = this.save(${entityLower});
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR, "新增失败"));
        return ${entityLower}.getId();
    }

    @Override
    public boolean delete${entity}(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ${entity} old${entity} = this.getById(id);
        ThrowUtils.throwIf(old${entity} == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据不存在"));
        return this.removeById(id);
    }

    @Override
    public boolean update${entity}(${entity}UpdateRequest ${entityLower}UpdateRequest) {
        ThrowUtils.throwIf(${entityLower}UpdateRequest == null || ${entityLower}UpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        Long id = ${entityLower}UpdateRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ${entity} old${entity} = this.getById(id);
        ThrowUtils.throwIf(old${entity} == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据不存在"));
        ${entity} ${entityLower} = ${entity}Convert.INSTANCE.toEntity(${entityLower}UpdateRequest);
        valid${entity}(${entityLower}, false);
        return this.updateById(${entityLower});
    }

    @Override
    public ${entity}VO get${entity}VOById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ${entity} ${entityLower} = this.getById(id);
        ThrowUtils.throwIf(${entityLower} == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "数据不存在"));
        return ${entity}Convert.INSTANCE.toVO(${entityLower});
    }

    @Override
    public Page<${entity}VO> list${entity}VOByPage(${entity}QueryRequest ${entityLower}QueryRequest) {
        ThrowUtils.throwIf(${entityLower}QueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = ${entityLower}QueryRequest.getCurrent() == null ? 1L : ${entityLower}QueryRequest.getCurrent();
        long pageSize = ${entityLower}QueryRequest.getPageSize() == null ? 10L : ${entityLower}QueryRequest.getPageSize();
        ThrowUtils.throwIf(current <= 0 || pageSize <= 0, ErrorCode.PARAMS_ERROR, "分页参数错误");

        LambdaQueryWrapper<${entity}> queryWrapper = buildQueryWrapper(${entityLower}QueryRequest);
        Page<${entity}> ${entityLower}Page = this.page(new Page<>(current, pageSize), queryWrapper);

        Page<${entity}VO> ${entityLower}VOPage = new Page<>(
                ${entityLower}Page.getCurrent(),
                ${entityLower}Page.getSize(),
                ${entityLower}Page.getTotal()
        );
        List<${entity}VO> ${entityLower}VOList = ${entity}Convert.INSTANCE.toVOList(${entityLower}Page.getRecords());
        ${entityLower}VOPage.setRecords(${entityLower}VOList);
        return ${entityLower}VOPage;
    }

    /**
     * 查询条件构造
     */
    private LambdaQueryWrapper<${entity}> buildQueryWrapper(${entity}QueryRequest ${entityLower}QueryRequest) {
        LambdaQueryWrapper<${entity}> queryWrapper = new LambdaQueryWrapper<>();
<#list queryExactFields as field>
        queryWrapper.eq(${entityLower}QueryRequest.get${field.propertyName?cap_first}() != null,
                ${entity}::get${field.propertyName?cap_first}, ${entityLower}QueryRequest.get${field.propertyName?cap_first}());
</#list>
<#list queryTextFields as field>
        queryWrapper.like(StringUtils.isNotBlank(${entityLower}QueryRequest.get${field.propertyName?cap_first}()),
                ${entity}::get${field.propertyName?cap_first}, ${entityLower}QueryRequest.get${field.propertyName?cap_first}());
</#list>
<#if queryExactFields?size == 0 && queryTextFields?size == 0>
        // 按需补充查询条件
</#if>
        queryWrapper.orderByDesc(${entity}::getId);
        return queryWrapper;
    }

    /**
     * 数据校验
     */
    private void valid${entity}(${entity} ${entityLower}, boolean add) {
        ThrowUtils.throwIf(${entityLower} == null, ErrorCode.PARAMS_ERROR);
        if (!add) {
            ThrowUtils.throwIf(${entityLower}.getId() == null || ${entityLower}.getId() <= 0, ErrorCode.PARAMS_ERROR);
        }
        // 按需补充唯一性、状态流转、业务字段合法性等校验
    }
}
