package com.campus.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台后端启动类。
 *
 * <p>启动后会自动创建 campus_trade 数据库与全部表结构，并写入初始数据（详见 resources/db/*.sql）。</p>
 */
@SpringBootApplication
@MapperScan("com.campus.trade.mapper")
@EnableScheduling
public class CampusTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
