package com.campustrade.mapper;

import com.campustrade.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    Report selectById(@Param("id") Long id);

    List<Report> selectByReporterId(@Param("reporterId") Long reporterId, @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    List<Report> selectList(@Param("status") String status, @Param("targetType") Integer targetType,
                            @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("status") String status, @Param("targetType") Integer targetType);

    int insert(Report report);

    int updateById(Report report);

    List<Report> selectAllByStatus(@Param("keyword") String keyword, @Param("status") String status,
                                   @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByStatus(@Param("keyword") String keyword, @Param("status") String status);
}