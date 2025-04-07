# Java專案實務－蒼穹點餐

## 1.專案介紹

### 1.1 專案介紹

本專案是專為餐飲企業（餐廳、飯店）客製化的軟體產品，包括系統管理後台 和 客戶點餐應用 兩部分。其中系統管理後台主要提供給餐飲企業內部員工使用，可以對餐廳的分類、菜色、套餐、訂單、員工等進行管理維護。客戶端主要提供給消費者使用，可在線上瀏覽菜色、新增購物車、下單等。

### 1.2 開發步驟

本專案分為兩個模組進行開發，分別是基礎資料模組、點餐業務模組。

後台管理端開發主要是針對系統管理後台實現基本需求，如員工登入、新增員工、新增菜色、修改菜色、新增套餐、刪除套餐、查詢訂單等。

使用者端開發主要是基於使用者的基本使用需求實現，如 Line 登入 , 菜色規查詢 , 購物車功能 , 下單 , 分類及菜餚瀏覽。

### 1.3 技術選型

**後端：**

1. 對Maven工程的結構和特點需要有一定的理解
2. git: 版本控制工具, 在團隊協作中, 使用該工具對專案中的程式碼進行管理。
3. junit：單元測試工具，開發人員功能完成後，需透過junit對功能進行單元測試。
4. postman: 介面測工具，模擬使用者發起的各類HTTP請求，取得對應的回應結果。
5. SpringBoot： 快速建構Spring專案, 採用 "約定優於配置" 的想法, 簡化Spring專案的配置開發。
6. SpringMVC：SpringMVC是spring框架的一個模組，springmvc和spring無需透過中間整合層進行整合，可以無縫集成。
7. JWT: 用於對應用程式上的使用者進行身份驗證的標記。
8. Swagger： 可以自動的幫助開發人員產生介面文檔，並對介面進行測試。
9. MySQL： 關係型資料庫, 本專案的核心業務資料都會採用MySQL進行儲存。
10. Redis： 基於key-value格式儲存的記憶體資料庫, 存取速度快, 經常使用它做快取。
11. Mybatis： 本專案持久層將會使用Mybatis開發。
12. pagehelper: 分頁外掛程式。
13. spring data redis: 簡化java程式碼操作Redis的API。

## 2.專案重點功能的實現想法以及程式碼

### 2.1基礎數據模組重點

#### 2.1.1 完善登入認證功能

1.定義攔截器

```java
/**
 * jwt權杖校驗的攔截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校驗jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判斷目前攔截到的是Controller的方法還是其他資源
        if (!(handler instanceof HandlerMethod)) {
            //目前攔截到的不是動態方法，直接放行
            return true;
        }

        //1、從請求頭中取得權杖
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校驗權杖
        try {
            log.info("jwt校驗:{}", token);
            //使用jwt工具類別從請求頭token中解析權杖
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("當前員工id：", empId);
            BaseContext.setCurrentId(empId);
            //3、通過，放行
            return true;
        } catch (Exception ex) {
            //4、不通過，響應401狀態碼
            response.setStatus(401);
            return false;
        }
    }
}
```

2.在MVC配置類別中註冊自訂攔截器

```java
/**
 * 配置類，註冊web層相關組件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 註冊自訂攔截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("開始註冊自訂攔截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")//攔截/admin下的所有資源
                .excludePathPatterns("/admin/employee/login");//放行/admin/employee/login的資源

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")//放行用戶登錄
                .excludePathPatterns("/user/shop/status");//放行查詢店鋪狀態的請求
    }
}
```

該功能是通過使用自訂攔截器的前置攔截方法對訪問路徑進行攔截，通過校驗請求頭token中攜帶的jwt權杖來判斷用戶是否已經完成登錄，如果沒有登錄則自動跳轉到登錄頁面。需要攔截或者需要放行的路徑可在配置類中註冊攔截器時一同配置。

#### 2.1.2 定義全域異常處理器

1.捕獲業務異常或者其他自訂異常

```java
/**
 * 全域異常處理器，處理專案中拋出的業務異常
 */
@RestControllerAdvice //該類為全域異常處理器
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕獲業務異常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("異常資訊：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕獲SQL異常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        //Duplicate entry 'zhangsan' for key 'idx_username',唯一約束異常
        //獲取異常資訊與唯一異常資訊中的關鍵字進行比較
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        } else {
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
```

 Controller拋出的異常沒有處理，最終會拋給全域異常處理器處理，處理完後再給流覽器回應統一處理結果集。流覽器拿到這個處理結果集後，會對結果進行處理然後以警告提示的形式展示到頁面上。就比如在新增員工時，當新增一個資料庫中已經存在的員工名字，由於表中已經對員工名字做了唯一約束，因此會拋出資料庫的唯一約束異常，這時就可以通過Controller拋出這個沒有處理異常到全域異常處理器，全域異常處理器對該異常進行處理然後回應給前端，前端拿到這個處理結果就可以做相應的處理。

#### 2.1.3 分頁查詢

1.導入pagehelper的起步依賴

2.接收請求參數的實體類

```java
@Data
public class EmployeePageQueryDTO implements Serializable {

    //員工姓名
    private String name;

    //頁碼
    private int page;

    //每頁顯示記錄數
    private int pageSize;

}
```



3.使用PageHelper外掛程式實現分頁查詢

```java
/**
     * 員工分頁查詢
     *
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        //select * from employee limit page,pageSize,使用PageHelper外掛程式可以實現limit關鍵字的自動拼接

        //使用PageHelper的startPage方法設置好分頁參數
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        //接收查詢返回的Page集合,在調用Page提供的方法時可以實現自動查詢(mapper層不用寫查詢語句)
        Page<Employee> p = employeeMapper.list(employeePageQueryDTO);
        //使用Page集合提供的方法獲取查詢的總記錄數和當前頁數的資料集合
        long total = p.getTotal();
        List<Employee> records = p.getResult();
        for (Employee employee : records) {
            employee.setPassword("****");
        }
        PageResult pageResult = new PageResult(total, records);

        return pageResult;
    }
```

此代碼通過前端頁面發送ajax請求，將分頁查詢參數（page、pageSize、name）提交到服務端，服務端把參數傳遞到Service層，Service層通過PageHelper外掛程式的startPage方法設置好分頁參數（page、pageSize），在進行查詢時，PageHelper外掛程式就可以實現sql語句中limit關鍵字的自動拼接，從而實現自動查詢的效果，查詢結束後，用PageHelper中的Page物件接收，通過Page物件獲取查詢的資料以及查詢的到的記錄的總數，然後把資料封裝成結果集返回給前端即可。

#### 2.1.4 擴展消息轉換器

1.自訂對象轉換器

```java
/**
 * 對象映射器:基於jackson將Java對象轉為json，或者將json轉為Java對象
 * 將JSON解析為Java物件的過程稱為 [從JSON反序列化Java物件]
 * 從Java物件生成JSON的過程稱為 [序列化Java物件到JSON]
 */
public class JacksonObjectMapper extends ObjectMapper {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    //public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public JacksonObjectMapper() {
        super();
        //收到未知屬性時不報異常
        this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        //反序列化時，屬性不存在的相容處理
        this.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        //註冊功能模組 例如，可以添加自訂序列化器和反序列化器
        this.registerModule(simpleModule);
    }
}
```

2.擴展消息轉換器

```java
/**
     * springMVC提供的消息轉化器,統一對後端傳給前端的時間資料格式化
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("擴展消息轉換器...");
        //創建一個消息轉換器物件
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要為消息轉換器設置一個物件轉換器,把java物件序列化為json資料
        //JacksonObjectMapper 自己定義的一個消息轉換器類
        converter.setObjectMapper(new JacksonObjectMapper());
        //將自己的消息轉換器加入到轉化器的容器中,添加索引確保容器優先使用我們自己定義的消息轉換器
        converters.add(0,converter);
    }
```

此自訂的對象轉換器主要是解決前端頁面js處理long型數位精度丟失的問題，由於js處理long型數位只能精確到前16位，所以通過ajax發送請求提交給服務端的id就會變，進而導致提交的id和資料庫中的id不一致。因此就可以使用此轉換器，首先是創建了這個物件轉換器JacksonobjectMapper，基於Jackson進行Java物件到json資料的轉換，然後在WebMvcConfig配置類中擴展Spring mvc的消息轉換器，在此消息轉換器中使用提供的物件轉換器進行Java物件到json資料的轉換，最後在服務端給前端返回json資料時進行處理，將Long型資料統一轉為String字串類型即可解決丟失精度的問題。

#### 2.1.5 使用ThreadLocal緩存當前使用者的唯一標識

1.定義ThreadLocal工具類

```java
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
```

2.在攔截器校驗權杖時把從權杖中解析出來的員工id緩存到ThreadLocal中

```java
//2、校驗權杖
        try {
            log.info("jwt校驗:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("當前員工id：", empId);
            BaseContext.setCurrentId(empId);
            //3、通過，放行
            return true;
        } catch (Exception ex) {
            //4、不通過，回應401狀態碼
            response.setStatus(401);
            return false;
        }
```

3.在新增和更新操作時通過ThreadLocal得到當前操作使用者的標識(以新增操作為例)

```java
/**
     * 新增員工
     *
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        //把EmployeeDTO物件裡的資料給到Employee物件
        //使用BeanUtils工具類裡的方法進行屬性拷貝
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        //設置帳號的狀態,預設給的是啟用狀態
        employee.setStatus(StatusConstant.ENABLE);

        //設置帳號的預設密碼,並且使用md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //設置目前記錄的創建時間和最後一次更新時間
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //設置記錄的創建者和修改者,即獲取當前登錄用戶的id

        //原始方法,通過HttpServletRequest物件獲取頭部的token再通過解析權杖獲取當前用戶id
//        //通過請求來獲取請求頭的資訊 token
//        String token = httpServletRequest.getHeader(jwtProperties.getAdminTokenName());
//        //解析jwt權杖中的內容
//        Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
//        //通過get方法根據key獲取value
//        Long id = (Long) claims.get(JwtClaimsConstant.EMP_ID);

        //新方法,通過ThreadLocal提供的set get方法對資料進行存取
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }
```

通過代碼注釋可以看到，當我們為創建者、更新者賦值時，往往需要通過HttpServletRequest物件獲取頭部的token再通過解析權杖獲取當前用戶id，這種做法十分繁瑣，大大增加了代碼量。此時我們可以利用ThreadLocal的特性，當攔截器攔截請求校驗權杖時，在解析權杖的同時把當前使用者的唯一標識存入ThreadLocal中，當我們需要為創建者、更新者賦值時，從ThreadLocal中取出當前使用者的唯一標識，這樣可以大大減少了代碼量，同時解決多個層之間傳遞當前使用者標識困難的問題。

#### 2.1.6 公共欄位的自動填充

1.定義枚舉類

```java
/**
 * 資料庫操作類型
 */
public enum OperationType {

    /**
     * 更新操作
     */
    UPDATE,

    /**
     * 插入操作
     */
    INSERT

}
```

2.定義自動填充注解類

```java
/**
 * @author 喜歡悠然獨自在
 * @version 1.0
 * 自訂注解,用於標記某個方法需要進行公共欄位的自動填充
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //定義枚舉類物件,內含資料庫操作類型:UPDATE INSERT
    OperationType value();

}
```

3.定義切面類，使用前置通知

```java
@Aspect//切面類
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 前置通知,攔截到方法後在方法執行前在通知中為公共欄位賦值
     *
     * @param joinPoint 連接點
     */
    //切入點運算式1 攔截mapper包下所有的類以及所有的方法
    //切入點運算式2 攔截有@AutoFill注解的方法
    @Before("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFill(JoinPoint joinPoint) {
        log.info("開始進行公共欄位的自動填充...");

        //通過連接點獲取攔截到的方法的簽名物件
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //通過簽名物件獲取到方法然後再獲取方法上的注解物件
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        //通過注解對象獲取value裡的值
        OperationType operationType = autoFill.value();

        //通過連接點獲取當前攔截方法的參數物件陣列
        Object[] args = joinPoint.getArgs();
        //作防止空指針判斷
        if (args == null || args.length == 0) {
            return;
        }
        //取出裡面的參數
        Object entity = args[0];

        //準備要自動填充的資料
        LocalDateTime now = LocalDateTime.now();//當前時間 用於填充更新時間和創建時間
        Long id = BaseContext.getCurrentId();//操作人的id 用於填充更新人id和創建人id

        //根據注解中對應的不同類型為對應的屬性賦值 通過反射來獲得
        if (operationType == OperationType.INSERT) {
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通過反射為物件屬性賦值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, id);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
```

利用AOP切面程式設計的思想，把在spring中用於將那些與業務無關，但卻對多個物件產生影響的公共行為和邏輯，抽取公共模組複用，降低耦合，避免了大量重複代碼的出現。

#### 2.1.7 結合google cloud storage實現檔上傳的功能

1.導入google cloud storage的起步依賴

2.定義google cloud storage參數實體類

```java
@Component
@ConfigurationProperties(prefix = "sky.gcs")
@Data
public class GcsProperties {
    private String endpoint;
    //private String accessKeyId;
    //private String accessKeySecret;
    private String bucketName;
}
```

3.定義google cloud storage工具類

```java
@Data
@AllArgsConstructor
@Slf4j
@Component
public class GcsUtil {

    private final String endpoint;
    private final String bucketName;

    // 如果需要通過 GcsProperties 自動注入配置，您可以在構造函數中傳入 GcsProperties
    @Autowired
    public GcsUtil(GcsProperties gcsProperties) {
        this.endpoint = gcsProperties.getEndpoint();
        this.bucketName = gcsProperties.getBucketName();
    }

    /**
     * 文件上傳
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        // 使用 GCS 客戶端
        Storage storage = StorageOptions.newBuilder().setProjectId(bucketName).build().getService();

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);

            // 創建 Blob 信息（GCS 的對象）
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // 將文件內容上傳到 GCS
            Blob blob = storage.create(blobInfo, new ByteArrayInputStream(bytes));

            // 文件訪問路徑規則：gs://bucket-name/object-name
            String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("文件已上傳到: {}", fileUrl);

            return fileUrl;
        } catch (Exception e) {
            // 打印錯誤詳細信息
            log.error("文件上傳到 GCS 時發生錯誤，存儲桶: {}, 文件名: {}", bucketName, objectName, e);
            e.printStackTrace();
            return null;
        }
    }
}
```

4.定義gcS配置類，把google cloud storag工具類做成Bean交給Spring管理

```java
@Configuration
@Slf4j
public class GcsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GcsUtil gusUtil(GcsProperties gcsProperties){
        log.info("開始創建google雲文件上傳工具類對象: {}",gcsProperties);
        return new GcsUtil(gcsProperties.getEndpoint(),
                gcsProperties.getBucketName());
    }
}
```

該功能可以把本地的圖片上傳到google cloud storag，通過後端進行圖片位址的拼接後回應給前端，前端可以通過直接訪問該位址或者通過查詢資料庫得到該位址再訪問該位址來顯示圖片，同時把位址保存到資料庫，從而實現了本地檔的上傳。

### 2.2 點餐業務模組重點

#### 2.2.1 基於HttpClient實現line登錄

1.定義HttpClient工具類

```java
/**
 * Http工具類
 */
public class HttpClientUtil {

    static final  int TIMEOUT_MSEC = 5 * 1000;

    /**
     * 發送GET方式請求
     * @param url
     * @param paramMap
     * @return
     */
    public static String doGet(String url,Map<String,String> paramMap){
        // 創建Httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String result = "";
        CloseableHttpResponse response = null;

        try{
            URIBuilder builder = new URIBuilder(url);
            if(paramMap != null){
                for (String key : paramMap.keySet()) {
                    builder.addParameter(key,paramMap.get(key));
                }
            }
            URI uri = builder.build();

            //創建GET請求
            HttpGet httpGet = new HttpGet(uri);

            //發送請求
            response = httpClient.execute(httpGet);

            //判斷回應狀態
            if(response.getStatusLine().getStatusCode() == 200){
                result = EntityUtils.toString(response.getEntity(),"UTF-8");
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                response.close();
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 發送POST方式請求
     * @param url
     * @param paramMap
     * @return
     * @throws IOException
     */
    public static String doPost(String url, Map<String, String> paramMap) throws IOException {
        // 創建Httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";

        try {
            // 創建Http Post請求
            HttpPost httpPost = new HttpPost(url);

            // 創建參數列表
            if (paramMap != null) {
                List<NameValuePair> paramList = new ArrayList();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                }
                // 模擬表單
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
                httpPost.setEntity(entity);
            }

            httpPost.setConfig(builderRequestConfig());

            // 執行http請求
            response = httpClient.execute(httpPost);

            resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resultString;
    }

    /**
     * 發送POST方式請求
     * @param url
     * @param paramMap
     * @return
     * @throws IOException
     */
    public static String doPost4Json(String url, Map<String, String> paramMap) throws IOException {
        // 創建Httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";

        try {
            // 創建Http Post請求
            HttpPost httpPost = new HttpPost(url);

            if (paramMap != null) {
                //構造json格式資料
                JSONObject jsonObject = new JSONObject();
                for (Map.Entry<String, String> param : paramMap.entrySet()) {
                    jsonObject.put(param.getKey(),param.getValue());
                }
                StringEntity entity = new StringEntity(jsonObject.toString(),"utf-8");
                //設置請求編碼
                entity.setContentEncoding("utf-8");
                //設置資料類型
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }

            httpPost.setConfig(builderRequestConfig());

            // 執行http請求
            response = httpClient.execute(httpPost);

            resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resultString;
    }
    private static RequestConfig builderRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MSEC)
                .setConnectionRequestTimeout(TIMEOUT_MSEC)
                .setSocketTimeout(TIMEOUT_MSEC).build();
    }

}
```

2.使用HttpClient工具類調用Line服務介面實現Line登錄

```java
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //line 服務接口地址
    public static final String line_LOGIN = "https://api.line.me/oauth2/v2.1/verify";
    public static final String line_UserProfile = "https://api.line.me/v2/profile";
    @Autowired
    private LineProperties lineProperties;
    @Autowired
    private UserMapper userMapper;



    /**
     * Line 登入
     * @param userLoginDTO
     * @return
     */
    public User lineLogin(UserLoginDTO userLoginDTO){
        //調用line接口服務 獲得當前用戶的openid
        String accessToken = userLoginDTO.getAccess_token();
        String openid = userLoginDTO.getUser_id();
        //判斷openid 是否為空 如果為空表示登入失敗 拋出業務異常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判斷當前用戶是否為新用戶
        User user = userMapper.getByOpenid(openid);
        //如果是新用戶 自動完成註冊
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回這個用戶對象
        return user;
    }


}
```

這兩段代碼實現Line登錄的步驟：

1. 前端（如 Web 或 App）呼叫 LINE 授權介面，跳轉用戶至 LINE 授權頁面。

2. 用戶授權後，LINE 將帶著一組 code（授權碼）重導至前端設定的 redirect URI。
3. 前端收到 code，發送請求至後端伺服器，並攜帶 code。
4. 後端伺服器使用 code、Channel ID（client_id）、Channel Secret（client_secret）、redirect URI 等參數向 LINE 的 token API 發送請求，取得 access_token 與 id_token。
5. 後端解碼 id_token，獲取使用者的 LINE 資訊（如 userId，即 openid）。
6. 後端根據 userId 檢查該使用者是否已存在資料庫中：
7. 後端伺服器自訂登入態，生成包含 userId 的權杖（token），並將 token 及用戶資料回傳給前端。
8. 前端收到 token 後，將其存入 localStorage 或 cookie。
9. 前端後續呼叫後端 API 時，需在 request header 中附上 token（如 Authorization: Bearer ...）。
10. 後端收到請求後，解析 token，驗證使用者身份，驗證通過後執行對應的業務邏輯並返回結果。

#### 2.2.2 Redis + Spring Data Redis + Spring Cache實現緩存功能

1.導入Spring Data Redis、Spring Cache的起步依賴

2.編寫配置類，創建RedisTemplate物件

```java
@Configuration
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate redisTemplate = new RedisTemplate();
        //設置redis的連接工廠物件
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //設置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

}
```

當前配置類不是必須的，因為 Spring Boot 框架會自動裝配 RedisTemplate 物件，但是預設的key序列化器為

JdkSerializationRedisSerializer，導致我們存到Redis中後的資料和原始資料有差別，故設置為

StringRedisSerializer序列化器。

3.常用注解

在SpringCache中提供了很多緩存操作的注解，常見的是以下的幾個：

| **注解**       | **說明**                                                     |
| -------------- | ------------------------------------------------------------ |
| @EnableCaching | 開啟緩存注解功能，通常加在啟動類上                           |
| @Cacheable     | 在方法執行前先查詢緩存中是否有資料，如果有資料，則直接返回緩存資料；如果沒有緩存資料，調用方法並將方法返回值放到緩存中 |
| @CachePut      | 將方法的返回值放到緩存中                                     |
| @CacheEvict    | 將一條或多條資料從緩存中刪除                                 |

在spring boot專案中，使用緩存技術只需在專案中導入相關緩存技術的依賴包，並在啟動類上使用@EnableCaching開啟緩存支援即可。

例如，使用Redis作為緩存技術，只需要導入Spring data Redis的maven座標即可。

4.在使用者端介面SetmealController的 list 方法上加入@Cacheable注解

```java
/**
 * 根據分類id查詢套餐
 * @param categoryId
 * @return
 */
@GetMapping("/list")
@ApiOperation("根據分類id查詢套餐")
//查詢緩存中是否存在setmealCache::categoryId的緩存資料,有則直接返回,沒有則利用反射調用下面的方法,然後將返回值存入緩存
@Cacheable(cacheNames = "setmealCache",key = "#categoryId")
public Result<List<Setmeal>> list(Long categoryId) {
    log.info("根據分類id查詢套餐:{}",categoryId);
    Setmeal setmeal = Setmeal.builder()
            .categoryId(categoryId)
            .status(StatusConstant.ENABLE)//查詢啟售中的套餐
            .build();

    List<Setmeal> list = setmealService.list(setmeal);
    return Result.success(list);
}
```

5.在管理端介面SetmealController的 save、delete、update、startOrStop等方法上加入CacheEvict注解

```java
/**
 * 新增菜品
 *
 * @param setmealDTO
 * @return
 */
@PostMapping
@ApiOperation("新增套餐")
@CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId")
public Result save(@RequestBody SetmealDTO setmealDTO) {
    log.info("新增套餐:{}", setmealDTO);
    setmealService.saveWithDish(setmealDTO);
    return Result.success();
}
```

```java
/**
 * 套餐起售停售
 * @param status
 * @return
 */
@PostMapping("/status/{status}")
@ApiOperation("套餐起售停售")
@CacheEvict(cacheNames = "setmealCache",allEntries = true)
public Result startOrStop(@PathVariable Integer status,Long id) {
    log.info("套餐起售停售:{}{}",status,id);
    setmealService.startOrStop(status,id);
    return Result.success();
}
```

```java
@PutMapping
@ApiOperation("修改套餐")
@CacheEvict(cacheNames = "setmealCache",allEntries = true)
public Result update(@RequestBody SetmealDTO setmealDTO) {
    log.info("修改套餐：｛｝",setmealDTO);
    setmealService.updateWithSetmealDishes(setmealDTO);
    return Result.success();
}

@DeleteMapping
@ApiOperation("批量刪除")
@CacheEvict(cacheNames = "setmealCache",allEntries = true)
public Result delete(@RequestParam List<Long> ids) {
    log.info("批量刪除:{}",ids);
    setmealService.deleteBatch(ids);
    return Result.success();
}
```

Redis是一個基於**記憶體**的key-value結構資料庫。Redis 是互聯網技術領域使用最為廣泛的**存儲中介軟體**。使用Redis緩存資料可以減少前端部分頻繁的查詢請求請求到資料庫，從一定程度上減少了資料庫的壓力，使用Spring Cache提供的@Cacheable、@CacheEvict等注解簡化了保證資料一致性邏輯的開發。

#### 2.2.3 添加購物車功能

```java
/**
 * 添加購物車
 * @param shoppingCartDTO
 */
@Override
public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
    //判斷當前加入的商品購物車內是否存在了
    ShoppingCart shoppingCart = new ShoppingCart();
    BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);//ShoppingCartDTO中缺少使用者id屬性
    shoppingCart.setUserId(BaseContext.getCurrentId());//獲取當前登錄用戶的id並賦值
    List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

    //如果已經存在了,則該商品的數量+1
    if (list != null && list.size() > 0) {
        ShoppingCart sc = list.get(0);//一個使用者id只能查出一個購物車資料,所以直接獲取集合中第一條資料即可
        sc.setNumber(sc.getNumber() + 1);
        //更新資料
        shoppingCartMapper.updateById(sc);
    }else {
        //如果不存在,則需要插入一條購物車資料

        //判斷本次添加到購物車中的是菜品還是套餐
        Long dishId = shoppingCartDTO.getDishId();
        if (dishId != null) {//本次添加的是菜品
            Dish dish = dishMapper.selectById(dishId);
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        }else {//本次添加的是套餐
            Long setmealId = shoppingCartDTO.getSetmealId();
            Setmeal setmeal = setmealMapper.selectById(setmealId);
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        }
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(shoppingCart);

    }

}
```

主要邏輯是：先判斷當前加入的商品是否已存在，已存在則商品數量加1，不存在則判斷本次添加的商品是菜品還是套餐，是菜品則查菜品表，是套餐則查套餐表，對購車表的欄位屬性填充好後，新增資料到資料庫。

#### 2.2.4 刪除購物車中一個商品

```java
/**
 * 刪除購物車中一個商品
 * @param shoppingCartDTO
 */
@Override
public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
    //先查詢當前使用者id的購物車資料
    ShoppingCart shoppingCart = new ShoppingCart();
    BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
    shoppingCart.setUserId(BaseContext.getCurrentId());
    List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

    //判斷取出來是否有資料
    if (list != null && list.size() > 0) {
        ShoppingCart sc = list.get(0);//因為一個使用者只能查出來一個購物車資料,所以直接取第一個資料就行

        Integer number = sc.getNumber();//查看當前商品的數量
        if (number == 1) {
            //如果商品數量等於1,則直接刪除商品
            shoppingCartMapper.deleteById(sc.getId());
        } else {
            //如果商品數量大於1,則修改商品數量即可
            sc.setNumber(sc.getNumber() - 1);
            shoppingCartMapper.updateById(sc);

        }
    }
}
```

根據當前使用者id和前端傳過來的菜品id或者套餐id去查購物車表，一個使用者id根據一個菜品id或者套餐id只能查出一條資料，對這條資料的數量屬性進行判斷，大於1則數量減1，等於1則刪除該條資料。



### 3.項目總結

通過跟隨視頻完成了蒼穹外賣項目，我的Java程式設計水準得到了顯著提升。在這個過程中，我深入學習了Java語言的基礎知識，瞭解了Spring、Spring MVC、MyBatis等框架的使用，同時瞭解了前端技術，如Vue、ElementUI組件和前端三劍客等。

在項目中，我學到了如何搭建一個完整的點餐系統，從前端頁面的設計思想到後端邏輯的實現，涉及使用者註冊登錄、功能表流覽、下單支付等功能。通過視頻教程，我逐步理解了整個開發流程，包括專案需求分析、資料庫設計、系統架構搭建和閱讀介面文檔等方面。

特別是在使用SSM框架的過程中，我對Spring的IoC和AOP思想有了更深入的理解，同時學到了如何通過MyBatis進行資料庫操作，提高了對持久層的認識。在實際的開發中，我解決了許多bug，學到了如何調試代碼、處理異常，增強了對Java程式設計的實際操作能力。

雖然是跟著視頻學習，但是秉承著先學習，後實踐，然後再對照答案的思想，能自己嘗試實現的功能都自己嘗試實現出來後再看視頻講解，總能學到不少細節和程式設計思想，也加深了對各項技術的理解和掌握程度。

總體而言，通過完成蒼穹外賣項目，我不僅熟練掌握了Java語言的應用，還瞭解了企業級開發的一般流程和規範。這個項目是我Java學習的一個重要里程碑，為我今後深入學習和應用Java技術打下了堅實的基礎，讓我能夠在實際項目中不斷提升自己的程式設計能力。

參考視頻：[蒼穹外賣](https://www.bilibili.com/video/BV1TP411v7v6?p=1)
