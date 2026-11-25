package com.grad.csc.controller;

import com.grad.csc.common.BizException;
import com.grad.csc.common.PageResult;
import com.grad.csc.common.R;
import com.grad.csc.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 系统辅助接口：操作日志分页、文件上传。
 *
 * 文件上传统一落盘到项目根目录 uploads/，数据库只存相对路径 /uploads/xxx，
 * 前端永远不出现「手填图片 URL」的输入框（需求文档硬性要求）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final JdbcTemplate jdbc;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /** 操作日志分页 */
    @GetMapping("/system/log/page")
    public R<PageResult<Map<String, Object>>> logPage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_log WHERE deleted=0", Long.class);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id, user_name, module, action, method, params, ip, status, cost_ms, create_time
                FROM sys_log WHERE deleted=0 ORDER BY id DESC LIMIT ? OFFSET ?""",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("userName", rs.getString("user_name"));
                    m.put("module", rs.getString("module"));
                    m.put("action", rs.getString("action"));
                    m.put("method", rs.getString("method"));
                    m.put("params", rs.getString("params"));
                    m.put("ip", rs.getString("ip"));
                    m.put("status", rs.getInt("status"));
                    m.put("costMs", rs.getInt("cost_ms"));
                    java.sql.Timestamp t = rs.getTimestamp("create_time");
                    m.put("createTime", t == null ? null : t.toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    return m;
                }, s, (long) (p - 1) * s);
        return R.ok(PageResult.of(total == null ? 0 : total, p, s, rows));
    }

    /** 文件上传：返回相对路径，前端直接回填到表单字段 */
    @PostMapping("/file/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("上传文件为空");
        String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf").contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/gif/pdf 格式");
        }
        if (file.getSize() > 20L * 1024 * 1024) throw new BizException("单文件不能超过 20MB");

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir, date).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        String rel = "/uploads/" + date + "/" + fileName;
        jdbc.update("""
                INSERT INTO sys_file(file_name, file_path, file_size, file_type, uploader_id,
                    create_time, update_time, deleted)
                VALUES(?,?,?,?,?,NOW(),NOW(),0)""",
                name, rel, file.getSize(), file.getContentType(), UserContext.userId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", rel);
        m.put("path", rel);
        m.put("name", name);
        m.put("size", file.getSize());
        return R.ok(m);
    }
}
