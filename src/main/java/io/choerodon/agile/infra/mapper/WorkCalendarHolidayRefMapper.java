package io.choerodon.agile.infra.mapper;

import io.choerodon.agile.infra.dto.WorkCalendarHolidayRefDTO;
import io.choerodon.mybatis.common.BaseMapper;

import java.util.List;

/**
 * @author dinghuang123@gmail.com
 * @since 2018/10/9
 */
public interface WorkCalendarHolidayRefMapper extends BaseMapper<WorkCalendarHolidayRefDTO> {

    /**
     * 查询最新的年份
     *
     * @return String
     */
    Integer queryLastYear();

    /**
     * 根据年份查询节假日
     *
     * @param year year
     * @return WorkCalendarHolidayRefDTO
     */
    List<WorkCalendarHolidayRefDTO> queryWorkCalendarHolidayRelByYear(Integer year);

    /**
     * 根据年份查询节假日
     *
     * @param year year
     * @return WorkCalendarHolidayRefDTO
     */
    List<WorkCalendarHolidayRefDTO> queryWorkCalendarHolidayRelWithNextYearByYear(Integer year);

    /**
     * 根据年份查询工作日历，包含当年、去年、明年
     *
     * @param year year
     * @return WorkCalendarHolidayRefDTO
     */
    List<WorkCalendarHolidayRefDTO> queryByYearIncludeLastAndNext(Integer year);
}
