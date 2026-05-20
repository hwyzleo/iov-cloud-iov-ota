package net.hwyz.iov.cloud.iov.ota.service.facade.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.iov.ota.api.contract.ArticleMpt;
import net.hwyz.iov.cloud.iov.ota.api.feign.mpt.ArticleMptApi;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ArticleAppService;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.ArticleMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.repository.po.ArticlePo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/article")
public class ArticleMptController extends BaseController {

    private final ArticleAppService articleAppService;

    /**
     * 分页查询文章
     *
     * @param article 文章
     * @return 文章列表
     */
    @RequiresPermissions("ota:fota:article:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(ArticleMpt article) {
        logger.info("管理后台用户[{}]分页查询文章", SecurityUtils.getUsername());
        startPage();
        List<ArticlePo> articlePoList = articleAppService.search(article.getTitle(), article.getType(), getBeginTime(article), getEndTime(article));
        List<ArticleMpt> articleMptList = ArticleMptAssembler.INSTANCE.fromPoList(articlePoList);
        return getDataTable(articlePoList, articleMptList);
    }

    /**
     * 导出文章
     *
     * @param response 响应
     * @param article  文章
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:article:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, ArticleMpt article) {
        logger.info("管理后台用户[{}]导出文章", SecurityUtils.getUsername());
    }

    /**
     * 根据文章ID获取文章
     *
     * @param articleId 文章ID
     * @return 文章
     */
    @RequiresPermissions("ota:fota:article:query")
    @Override
    @GetMapping(value = "/{articleId}")
    public AjaxResult getInfo(@PathVariable Long articleId) {
        logger.info("管理后台用户[{}]根据文章ID[{}]获取文章", SecurityUtils.getUsername(), articleId);
        ArticlePo articlePo = articleAppService.getArticleById(articleId);
        return success(ArticleMptAssembler.INSTANCE.fromPo(articlePo));
    }

    /**
     * 新增文章
     *
     * @param article 文章
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:article:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ArticleMpt article) {
        logger.info("管理后台用户[{}]新增文章[{}]", SecurityUtils.getUsername(), article.getTitle());
        ArticlePo articlePo = ArticleMptAssembler.INSTANCE.toPo(article);
        articlePo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(articleAppService.createArticle(articlePo));
    }

    /**
     * 修改保存文章
     *
     * @param article 文章
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:article:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ArticleMpt article) {
        logger.info("管理后台用户[{}]修改保存文章[{}]", SecurityUtils.getUsername(), article.getTitle());
        ArticlePo articlePo = ArticleMptAssembler.INSTANCE.toPo(article);
        articlePo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(articleAppService.modifyArticle(articlePo));
    }

    /**
     * 删除文章
     *
     * @param articleIds 文章ID数组
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:article:remove")
    @Override
    @DeleteMapping("/{articleIds}")
    public AjaxResult remove(@PathVariable Long[] articleIds) {
        logger.info("管理后台用户[{}]删除文章[{}]", SecurityUtils.getUsername(), articleIds);
        return toAjax(articleAppService.deleteArticleByIds(articleIds));
    }
}
