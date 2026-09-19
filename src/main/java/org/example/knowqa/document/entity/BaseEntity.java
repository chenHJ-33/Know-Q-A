package org.example.knowqa.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {
    //创建时间
    @TableField(fill = FieldFill.INSERT)
    protected LocalDateTime createdAt;
    // 修改时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateAt;
    // 乐观锁版本号
    @Version
    protected Integer lockVersion;
    // 是否删除
    @TableLogic
    protected Integer deleted;

}
