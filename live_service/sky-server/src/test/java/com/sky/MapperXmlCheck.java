package com.sky;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 开发期校验工具：把每个 Mapper XML 交给 MyBatis 真正解析一遍。
 * <p>
 * 能查出编译期发现不了的问题：类型别名写错、resultType 类不存在、
 * parameterType 解析失败等。这些错误如果等到启动时才暴露，
 * 排查成本会高很多。
 * <p>
 * 运行方式（不需要 Spring、不需要数据库）：
 * javac -cp &lt;依赖&gt; -d &lt;输出目录&gt; MapperXmlCheck.java
 * java -cp &lt;输出目录&gt;:&lt;依赖&gt; com.sky.MapperXmlCheck
 */
public class MapperXmlCheck {

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        // 注册实体类别名，对应 application.yml 里的 type-aliases-package
        configuration.getTypeAliasRegistry().registerAliases("com.sky.entity");

        List<String> xmls = Arrays.asList(
                "ServiceItemMapper.xml",
                "ServiceSpecMapper.xml",
                "ServicePackageMapper.xml",
                "ServicePackageItemMapper.xml",
                "ProviderMapper.xml",
                "ProviderSkillMapper.xml",
                "SlotMapper.xml",
                "ReviewMapper.xml",
                "ServiceAreaMapper.xml",
                "ServiceOrderMapper.xml",
                "ServiceOrderItemMapper.xml");

        int fail = 0;
        for (String xml : xmls) {
            String resource = "mapper/" + xml;
            try (InputStream in = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
                System.out.println("[OK]   " + resource);
            } catch (Exception e) {
                System.out.println("[FAIL] " + resource + " -> " + e.getMessage());
                fail++;
            }
        }
        System.out.println(fail == 0
                ? ">>> 全部 " + xmls.size() + " 个 Mapper XML 均能被 MyBatis 正确解析"
                : ">>> " + fail + " 个 XML 解析失败");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
