package com.campus.trade.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理：把各类异常统一转换成 {"code":..,"message":..,"data":..}，并设置语义化 HTTP 状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：状态码与业务码由 ResultCode 决定 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        ResultCode rc = e.getResultCode();
        log.warn("业务异常 {} - {}", rc.getCode(), e.getMessage());
        return ResponseEntity.status(rc.getHttpStatus()).body(Result.fail(rc, e.getMessage()));
    }

    /** @Valid 校验失败（请求体）：返回字段级错误明细 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<List<Map<String, String>>>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getHttpStatus())
                .body(Result.fail(ResultCode.PARAM_ERROR, "参数校验失败", fieldErrors(e.getBindingResult().getFieldErrors())));
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<List<Map<String, String>>>> handleBind(BindException e) {
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getHttpStatus())
                .body(Result.fail(ResultCode.PARAM_ERROR, "参数校验失败", fieldErrors(e.getBindingResult().getFieldErrors())));
    }

    /** 缺少必填查询参数 / 参数类型不匹配 / JSON 格式错误 */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getHttpStatus())
                .body(Result.fail(ResultCode.PARAM_ERROR, "请求参数不合法：" + e.getMessage()));
    }

    /** 上传文件超过大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ResultCode.FILE_SIZE_EXCEEDED.getHttpStatus())
                .body(Result.fail(ResultCode.FILE_SIZE_EXCEEDED, "文件大小超过限制（单个文件最大 5MB）"));
    }

    /** 请求方法不支持，例如对只支持 POST 的接口发 GET */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(ResultCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(Result.fail(ResultCode.METHOD_NOT_ALLOWED, e.getMessage()));
    }

    /** Content-Type 不支持，例如把 JSON 请求头写成了 application/octet-stream（接口测试常见问题） */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(ResultCode.MEDIA_TYPE_NOT_SUPPORTED.getHttpStatus())
                .body(Result.fail(ResultCode.MEDIA_TYPE_NOT_SUPPORTED,
                        "请求 Content-Type 不支持：" + e.getContentType() + "，请改为 application/json"));
    }

    /** 访问不存在的接口或静态资源 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(ResultCode.NOT_FOUND.getHttpStatus())
                .body(Result.fail(ResultCode.NOT_FOUND, "请求的资源不存在：" + request.getRequestURI()));
    }

    /** 兜底：未预期的服务端异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e, HttpServletRequest request) {
        log.error("服务器内部错误 {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(ResultCode.SERVER_ERROR.getHttpStatus())
                .body(Result.fail(ResultCode.SERVER_ERROR, "服务器内部错误，请联系管理员"));
    }

    private List<Map<String, String>> fieldErrors(List<FieldError> errors) {
        return errors.stream().map(err -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", err.getField());
            item.put("message", err.getDefaultMessage());
            return item;
        }).toList();
    }
}
