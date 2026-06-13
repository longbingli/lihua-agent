package ${voPackage};

import lombok.Data;

import java.io.Serializable;
<#assign hasDate = false>
<#list voFields as field>
<#if field.propertyType == "Date">
<#assign hasDate = true>
</#if>
</#list>
<#if hasDate>
import java.util.Date;
</#if>

/**
 * ${table.comment!entity}视图
 */
@Data
public class ${entity}VO implements Serializable {

<#list voFields as field>
    /**
     * ${field.comment}
     */
    private ${field.propertyType} ${field.propertyName};

</#list>
    private static final long serialVersionUID = 1L;
}
