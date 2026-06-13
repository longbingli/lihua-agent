package ${convertPackage};

import ${dtoPackage}.${entity}AddRequest;
import ${dtoPackage}.${entity}EditRequest;
import ${dtoPackage}.${entity}UpdateRequest;
import ${entityPackage}.${entity};
import ${voPackage}.${entity}VO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * ${table.comment!entity}对象转换
 */
@Mapper
public interface ${entity}Convert {

    ${entity}Convert INSTANCE = Mappers.getMapper(${entity}Convert.class);

    ${entity} toEntity(${entity}AddRequest request);

    ${entity} toEntity(${entity}EditRequest request);

    ${entity} toEntity(${entity}UpdateRequest request);

    ${entity}VO toVO(${entity} entity);

    List<${entity}VO> toVOList(List<${entity}> entityList);
}
