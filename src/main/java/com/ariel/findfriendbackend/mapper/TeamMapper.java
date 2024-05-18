package com.ariel.findfriendbackend.mapper;

import com.ariel.findfriendbackend.model.domain.Team;
import com.ariel.findfriendbackend.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Ariel
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2024-05-13 21:27:38
* @Entity generator.domain.Team
*/
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    List<User> SelectUsers(@Param("teamId") long teamId);
}




