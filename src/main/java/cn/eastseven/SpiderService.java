package cn.eastseven;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
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

    private Site site = Site.me().setDomain("jd.com");

    @Autowired
    ProductRepository productRepository;

    final long time = 60 * 60 * 1000L;

    @Scheduled(fixedRate = time)
    public void start() {
        log.info("{}, Scheduled(fixedRate = {})", "spider start", time);
        Spider.create(this)
                .addUrl("http://www.jd.com/special.aspx?id=5")
                .addPipeline(new ConsolePipeline())
                .addPipeline(this)
                .thread(Runtime.getRuntime().availableProcessors())
                .run();
    }

    @Override
    public void process(Page page) {
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

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        List<Product> products = resultItems.get("products");

        List<Product> needSave = products.stream()
                .filter(product -> !productRepository.exists(product.getId()))
                .collect(Collectors.toList());

        productRepository.save(needSave);
        log.info("save {}, total {}", needSave.size(), productRepository.count());
    }
}
