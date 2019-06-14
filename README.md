# easy-journal
为 springBoot 项目打造的 Web 轻量级日志框架
导入 jar 包后即可在每次http请求后打印一条记录

目前记录分为三种类型，分别为成功、失败和警告

成功默认打印
```
 1 2019-06-14 02:52:56 uri:/api/getJson method:GET statusCode:200 ip:30.14.244.64 user:noUser mistiming:111ms
```
警告默认打印
```
2 2019-06-14 02:57:50 uri:/api/getb method:GET statusCode:410 ip:30.14.244.64 user:noUser requestHeader:{"cache-control":"no-cache","postman-token":"31ffd4c1-c4ee-4650-a003-0316852bfaee","content-type":"text/plain","user-agent":"PostmanRuntime/7.6.0","accept":"*/*","host":"localhost:8080","accept-encoding":"gzip, deflate","content-length":"26","connection":"keep-alive"} request:{"num":"1","count":"2"} mistiming:9ms
```
失败默认打印
```
5 2019-06-14 02:59:14 uri:/api/getJson method:GET statusCode:500 ip:30.14.244.64 user:noUser requestHeader:{"cache-control":"no-cache","postman-token":"603ec0da-23dc-4d4d-aaae-5573af621c4a","content-type":"text/plain","user-agent":"PostmanRuntime/7.6.0","accept":"*/*","host":"localhost:8080","accept-encoding":"gzip, deflate","content-length":"26","connection":"keep-alive"} request:{"num":"1","count":"2"} mistiming:46ms
```

你可以通过 ``@LogIgnore`` ``@LogAnyway``来控制请求的参数打印情况

被标注``@LogIgnore``的参数无论在任何情况下都不会被打印

被标注``@LogAnyway``的参数在任何情况下都会被打印

对于参数为类的方法中，可以在类上面添加标注``@BindingModel``来控制属性的打印情况

该标注有两个属性 ``isLogAnyway`` ``ignorePara``

``isLogAnyway`` 默认为false,当为true时在请求成功的情况下依然会打印请求参数

``ignorePara``接受参数名数组,包含在内的参数不会被打印（该行为会覆盖``isLogAnyway``）

同时你可以通过重写接口``JournalWapper`` 来自定义自己的打印行为

配置 ``journal.enabled`` 来控制日志的开关
配置 ``journal.logAnyway`` 来控制全局打印参数，建议在测试环境下开启
