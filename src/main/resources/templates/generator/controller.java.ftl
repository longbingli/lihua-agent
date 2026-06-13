package ${package.Controller};

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${basePackage}.common.BaseResponse;
import ${basePackage}.common.DeleteRequest;
import ${basePackage}.common.ErrorCode;
import ${basePackage}.common.ResultUtils;
import ${basePackage}.exception.BusinessException;
import ${dtoPackage}.${entity}AddRequest;
import ${dtoPackage}.${entity}QueryRequest;
import ${dtoPackage}.${entity}UpdateRequest;
import ${voPackage}.${entity}VO;
import ${package.Service}.${table.serviceName};
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ${table.comment!entity}控制器
 */
@RestController
@RequestMapping("/${entityLower}")
public class ${table.controllerName} {

    @Resource
    private ${table.serviceName} ${entityLower}Service;

    /**
     * 新增
     */
    @PostMapping("/add")
    public BaseResponse<Long> add${entity}(@RequestBody ${entity}AddRequest ${entityLower}AddRequest) {
        if (${entityLower}AddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long newId = ${entityLower}Service.add${entity}(${entityLower}AddRequest);
        return ResultUtils.success(newId, "创建成功");
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete${entity}(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = ${entityLower}Service.delete${entity}(deleteRequest.getId());
        return ResultUtils.success(result, "删除成功");
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> update${entity}(@RequestBody ${entity}UpdateRequest ${entityLower}UpdateRequest) {
        if (${entityLower}UpdateRequest == null || ${entityLower}UpdateRequest.getId() == null
                || ${entityLower}UpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = ${entityLower}Service.update${entity}(${entityLower}UpdateRequest);
        return ResultUtils.success(result, "更新成功");
    }

    /**
     * 根据 id 获取详情
     */
    @GetMapping("/get")
    public BaseResponse<${entity}VO> get${entity}VOById(@RequestParam("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ${entity}VO ${entityLower}VO = ${entityLower}Service.get${entity}VOById(id);
        return ResultUtils.success(${entityLower}VO);
    }

    /**
     * 分页查询
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<${entity}VO>> list${entity}VOByPage(
            @RequestBody ${entity}QueryRequest ${entityLower}QueryRequest) {
        if (${entityLower}QueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<${entity}VO> ${entityLower}VOPage = ${entityLower}Service.list${entity}VOByPage(${entityLower}QueryRequest);
        return ResultUtils.success(${entityLower}VOPage);
    }
}
