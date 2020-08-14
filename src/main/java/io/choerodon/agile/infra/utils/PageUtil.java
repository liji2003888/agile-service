package io.choerodon.agile.infra.utils;

import java.util.*;
import io.choerodon.mybatis.util.StringUtil;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.Sort;


/**
 * Created by WangZhe@choerodon.io on 2019-06-13.
 * Email: ettwz@hotmail.com
 */
public class PageUtil {

    public static Page buildPageInfoWithPageInfoList(Page page, List list) {
        Page result = new Page<>();
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        result.setTotalElements(page.getTotalElements());
        result.setContent(list);
        return result;
    }

    public static Sort sortResetOrder(Sort sort, String mainTableAlias, Map<String, String> map) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort != null) {
            Iterator<Sort.Order> iterator = sort.iterator();
            while (iterator.hasNext()) {
                boolean flag = false;
                Sort.Order order = iterator.next();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (entry.getKey().equals(order.getProperty())) {
//                        if(order.getDirection().isAscending()){
//                            order = Sort.Order.asc(entry.getValue());
//                        }
//                        else {
//                            order = Sort.Order.desc(entry.getValue());
//                        }
                        order.setProperty(entry.getValue());
                        flag = true;
                    }
                }
                if (!flag) {
                    //驼峰转下划线
                    if(order.getDirection().isAscending()){
                        if (mainTableAlias != null) {
                            order.setProperty(mainTableAlias + "." + StringUtil.camelhumpToUnderline(order.getProperty()));
                        } else {
                            order.setProperty(StringUtil.camelhumpToUnderline(order.getProperty()));
                        }
                    } else {
                        if (mainTableAlias != null) {
                            order.setProperty(mainTableAlias + "." + StringUtil.camelhumpToUnderline(order.getProperty()));
                        } else {
                            order.setProperty(StringUtil.camelhumpToUnderline(order.getProperty()));
                        }
                    }
                }
                orders.add(order);
            }
        }
        return new Sort(orders);
    }

    public static int getBegin(int page, int size) {
        page++;
        page = page <= 1 ? 1 : page;
        return (page - 1) * size;
    }

    public static Page emptyPageInfo(int page, int size) {
        Page result = new Page();
        result.setNumber(page);
        result.setSize(size);
        return result;
    }

    public static Page getPageInfo(int page, int size, int total, Collection list) {
        Page result = new Page();
        result.setNumber(page);
        result.setSize(size);
        result.setTotalElements(total);
        result.setContent(Arrays.asList(list.toArray()));
        return result;
    }

}
