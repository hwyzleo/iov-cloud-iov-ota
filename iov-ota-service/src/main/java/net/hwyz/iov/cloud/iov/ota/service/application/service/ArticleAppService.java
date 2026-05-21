package net.hwyz.iov.cloud.iov.ota.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.mapper.ArticleMapper;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ArticlePo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAppService {

    private final ArticleMapper articleDao;

    /**
     * 查询文章
     *
     * @param title     文章标题
     * @param type      文章类型
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 升级活动列表
     */
    public List<ArticlePo> search(String title, Integer type, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", ParamHelper.fuzzyQueryParam(title));
        map.put("type", type);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return articleDao.selectPoByMap(map);
    }

    /**
     * 根据主键ID获取文章
     *
     * @param id 主键ID
     * @return 文章
     */
    public ArticlePo getArticleById(Long id) {
        return articleDao.selectPoById(id);
    }

    /**
     * 新增文章
     *
     * @param article 文章
     * @return 结果
     */
    public int createArticle(ArticlePo article) {
        return articleDao.insertPo(article);
    }

    /**
     * 修改文章
     *
     * @param article 文章
     * @return 结果
     */
    public int modifyArticle(ArticlePo article) {
        return articleDao.updatePo(article);
    }

    /**
     * 批量删除文章
     *
     * @param ids 文章ID数组
     * @return 结果
     */
    public int deleteArticleByIds(Long[] ids) {
        return articleDao.batchPhysicalDeletePo(ids);
    }

}
