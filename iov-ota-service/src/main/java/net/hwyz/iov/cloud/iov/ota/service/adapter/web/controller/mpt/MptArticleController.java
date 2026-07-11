package net.hwyz.iov.cloud.iov.ota.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.ota.api.contract.ArticleMpt;
import net.hwyz.iov.cloud.iov.ota.service.application.service.ArticleAppService;
import net.hwyz.iov.cloud.iov.ota.service.facade.assembler.ArticleMptAssembler;
import net.hwyz.iov.cloud.iov.ota.service.infrastructure.persistence.po.ArticlePo;
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
@RequestMapping(value = "/api/mpt/article/v1")
public class MptArticleController extends BaseController {

    private final ArticleAppService articleAppService;

    /**
     * 分页查询文章
     *
     * @param article 文章
     * @return 文章列表
     */
    @RequiresPermissions("ota:fota:article:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<ArticleMpt>> list(ArticleMpt article) {
        log.info("管理后台用户[{}]分页查询文章", SecurityUtils.getUsername());
        startPage();
        List<ArticlePo> articlePoList = articleAppService.search(article.getTitle(), article.getType(), getBeginTime(article), getEndTime(article));
        return ApiResponse.ok(getPageResult(PageUtil.convert(articlePoList, ArticleMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出文章
     *
     * @param response 响应
     * @param article  文章
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("ota:fota:article:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, ArticleMpt article) {
        log.info("管理后台用户[{}]导出文章", SecurityUtils.getUsername());
    }

    /**
     * 根据文章ID获取文章
     *
     * @param articleId 文章ID
     * @return 文章
     */
    @RequiresPermissions("ota:fota:article:query")
    @GetMapping(value = "/{articleId}")
    public ApiResponse<ArticleMpt> getInfo(@PathVariable Long articleId) {
        log.info("管理后台用户[{}]根据文章ID[{}]获取文章", SecurityUtils.getUsername(), articleId);
        ArticlePo articlePo = articleAppService.getArticleById(articleId);
        return ApiResponse.ok(ArticleMptAssembler.INSTANCE.fromPo(articlePo));
    }

    /**
     * 新增文章
     *
     * @param article 文章
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ota:fota:article:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody ArticleMpt article) {
        log.info("管理后台用户[{}]新增文章[{}]", SecurityUtils.getUsername(), article.getTitle());
        ArticlePo articlePo = ArticleMptAssembler.INSTANCE.toPo(article);
        articlePo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(articleAppService.createArticle(articlePo));
    }

    /**
     * 修改保存文章
     *
     * @param article 文章
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ota:fota:article:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody ArticleMpt article) {
        log.info("管理后台用户[{}]修改保存文章[{}]", SecurityUtils.getUsername(), article.getTitle());
        ArticlePo articlePo = ArticleMptAssembler.INSTANCE.toPo(article);
        articlePo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(articleAppService.modifyArticle(articlePo));
    }

    /**
     * 删除文章
     *
     * @param articleIds 文章ID数组
     * @return 结果
     */
    @Log(title = "FOTA文章管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ota:fota:article:remove")
    @DeleteMapping("/{articleIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] articleIds) {
        log.info("管理后台用户[{}]删除文章[{}]", SecurityUtils.getUsername(), articleIds);
        return ApiResponse.ok(articleAppService.deleteArticleByIds(articleIds));
    }
}
