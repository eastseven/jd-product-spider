package cn.eastseven;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.*;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dongqi on 17/3/29.
 */
@Slf4j
@Service
public class SpiderService implements PageProcessor, Pipeline {

    private Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");

    @Autowired
    ProductRepository productRepository;

    final long time = 60 * 60 * 1000L;

    final String special = "http://www.jd.com/special.aspx?id=5";
    final String allSort = "https://www.jd.com/allSort.aspx";

    @Scheduled(fixedRate = time)
    public void start() {
        log.info("{}, Scheduled(fixedRate = {})", "spider start", time);
        Spider.create(this)
                .addUrl(allSort, special)
                .addPipeline(new ConsolePipeline())
                .addPipeline(this)
                .thread(Runtime.getRuntime().availableProcessors())
                .run();
    }

    @Override
    public void process(Page page) {
        log.info("1={}", page.getUrl());
        if (special.equals(page.getUrl())) {
            log.info("2={}", page.getUrl());
            processProduct(page);
        } else if (allSort.equals(page.getUrl())) {
            log.info("3={}", page.getUrl());
            processCategory(page);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        if (resultItems.getAll().isEmpty()) return;

        try {
            List<Product> products = resultItems.get("products");
            List<Product> needSave = products.stream()
                    .filter(product -> !productRepository.exists(product.getId()))
                    .collect(Collectors.toList());

            productRepository.save(needSave);
            log.info("save {}, total {}", needSave.size(), productRepository.count());
        } catch (Exception e) {
            log.error("", e);
        }
    }

    void processProduct(Page page) {
        Element body = page.getHtml().getDocument().body();

        List<Product> products = Lists.newArrayList();
        body.select("li").stream().filter(li -> li.hasAttr("sku")).forEach(sku -> {
            String image = sku.select("div.p-img a img").attr("data-lazyload");
            String name = sku.select("div.p-name a").text();
            String url = sku.select("div.p-name a").attr("href");
            String skuid = sku.select("div.p-price").attr("sku");

            products.add(Product.builder().id(skuid).name(name).image(image).url(url).build());
        });

        log.debug("{}", products.size());
        page.putField("products", products);
    }

    void processCategory(Page page) {
        log.debug("{}", page);
        Elements items = page.getHtml().getDocument().body().select("div.items div.clearfix dd a");
        items.stream().forEach(a -> {
            String url = a.attr("href");
            log.debug("{}", url);
        });

    }
}
