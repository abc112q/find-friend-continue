package com.ariel.findfriendbackend.controller;

import com.ariel.findfriendbackend.common.BaseResponse;
import com.ariel.findfriendbackend.common.ErrorCode;
import com.ariel.findfriendbackend.common.ResultUtils;
import com.ariel.findfriendbackend.dto.TeamQuery;
import com.ariel.findfriendbackend.exception.BusinessException;
import com.ariel.findfriendbackend.mapper.TeamMapper;
import com.ariel.findfriendbackend.model.domain.Team;
import com.ariel.findfriendbackend.model.domain.User;
import com.ariel.findfriendbackend.model.domain.UserTeam;
import com.ariel.findfriendbackend.model.request.*;
import com.ariel.findfriendbackend.model.vo.TeamUserVO;
import com.ariel.findfriendbackend.model.vo.UserVO;
import com.ariel.findfriendbackend.service.TeamService;
import com.ariel.findfriendbackend.service.UserService;
import com.ariel.findfriendbackend.service.UserTeamService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ariel.findfriendbackend.contant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Ariel
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000/"})
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private TeamMapper teamMapper;

    /**
     * 全局异常处理和拦截器进行权限校验的区别
     * 全局异常处理：已经走进方法了，才发现这个人没有权限然后报错
     * 拦截器：还没具体到执行逻辑就发现他没权限，然后拦截
     *
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamAddRequest.getCurrentId();
        log.info("id:" + currentId);
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        log.info("team:" + team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        log.info("team:" + teamUpdateRequest);
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamUpdateRequest.getCurrentId();
        log.info("id:" + currentId);
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
        log.info("loginUser:" + loginUser);
//        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        // 1、查询队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 2、判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {
        }
        // 3、查询已加入队伍的人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍 id => 加入这个队伍的用户列表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));
        return ResultUtils.success(teamList);
    }

    // todo 查询分页
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 加入的队伍
     *
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamJoinRequest.getCurrentId();
        log.info("id:" + currentId);
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
//        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamQuitRequest.getCurrentId();
        log.info("id:" + currentId);
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
//        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 解散队伍
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        log.info("deleteRequest:" + deleteRequest);
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }


    /**
     * 获取我创建的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamQuery.getCurrentId();
        log.info("id:" + teamQuery.getCurrentId());
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
//        User loginUser = userService.getLoginUser(request);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", loginUser.getId());
        String searchText = teamQuery.getSearchText();
        log.info("searchText" + searchText);
//            如果描述或者标题中包含相关内容，模糊查询
        if (StringUtils.isNotBlank(searchText)) {
            teamQueryWrapper.and(qw -> qw.like("description", searchText).or().like("name", searchText));
        }
        List<Team> list = teamService.list(teamQueryWrapper);
        if (list == null) {
            return ResultUtils.success(null);
        }
        List<TeamUserVO> teamList = new ArrayList<>();
//        1、通过队伍查询结果遍历获取每个队伍id
        for (Team team : list) {
            Long teamId = team.getId();
//          2、通过队伍查询创建人信息（塞入UserTeamVo）中
            Long CreateUserId = team.getUserId();
            UserVO createUserVO = new UserVO();
            User CreateUserById = userService.getById(CreateUserId);
            TeamUserVO teamUserVO = new TeamUserVO();
//            将队伍信息拷贝到teamUserVO中
            BeanUtils.copyProperties(team, teamUserVO);
//            将创建人用户信息脱敏
            BeanUtils.copyProperties(CreateUserById, createUserVO);
            teamUserVO.setCreateUser(createUserVO);
//          3、通过队伍id在队伍用户表中查询出用户关系集合
            List<User> users = teamMapper.SelectUsers(teamId);
            List<UserVO> Users = new ArrayList<>();
            for (User user : users) {
//          4、遍历集合获取用户id得到用户
//                如果用户存在
                if (user != null) {
                    UserVO userVO = new UserVO();
//          5、用户脱敏
                    BeanUtils.copyProperties(user, userVO);
                    Users.add(userVO);
                }
            }
            teamUserVO.setUserVOS(Users);
            teamList.add(teamUserVO);
        }
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
//    @GetMapping("/list/my/join")
//    public BaseResponse<List<TeamUserVo>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
//        if (teamQuery == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        String currentId = teamQuery.getCurrentId();
//        log.info("id:"+currentId);
//        String redisKey = String.format(USER_LOGIN_STATE+currentId);
//        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
//        User loginUser = (User) valueOperations.get(redisKey);
//        //User loginUser = userService.getLoginUser(request);
//        log.info("loginUser:"+loginUser.toString());
//        log.info(teamQuery.getSearchText());
//        List<TeamUserVo> myJoinTeam = teamService.getMyJoinTeam(loginUser,teamQuery);
//        return ResultUtils.success(myJoinTeam);
//    }
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String currentId = teamQuery.getCurrentId();
        log.info("id:" + currentId);
        String redisKey = String.format(USER_LOGIN_STATE + currentId);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        User loginUser = (User) valueOperations.get(redisKey);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }


}
