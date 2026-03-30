/*
  knowledge_seed.sql
  用于本地快速初始化旅行知识库演示数据（景点 + 美食）
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 先清理旧命名与新命名，确保结构一致
DROP TABLE IF EXISTS `tb_knowledge_attraction`;
DROP TABLE IF EXISTS `tb_knowledge_food`;
DROP TABLE IF EXISTS `knowledge_attraction`;
DROP TABLE IF EXISTS `knowledge_food`;

-- =============================
-- 景点知识库表
-- =============================
CREATE TABLE IF NOT EXISTS `knowledge_attraction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(128) NOT NULL COMMENT '景点名称',
  `description` TEXT COMMENT '景点描述',
  `category` VARCHAR(64) COMMENT '类型：自然/文化/温泉/美食/休闲',
  `season` VARCHAR(32) COMMENT '最佳季节：春/夏/秋/冬/全年',
  `suitable_for` VARCHAR(64) COMMENT '适合人群：家庭/情侣/老人/儿童/年轻人',
  `price_level` INT COMMENT '价格等级：1-5',
  `rating` DOUBLE COMMENT '评分（0-5）',
  `tags` JSON COMMENT '标签JSON数组',
  `address` VARCHAR(255) COMMENT '地址',
  `opening_hours` VARCHAR(128) COMMENT '营业时间',
  `ticket_price` VARCHAR(128) COMMENT '门票价格',
  `features` JSON COMMENT '特色亮点JSON数组',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景点知识库';

-- =============================
-- 美食知识库表
-- =============================
CREATE TABLE IF NOT EXISTS `knowledge_food` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(128) NOT NULL COMMENT '美食名称',
  `description` TEXT COMMENT '美食描述',
  `category` VARCHAR(64) COMMENT '类型：主食/小吃/特产/饮品',
  `taste` VARCHAR(64) COMMENT '口味：甜/咸/辣/酸/鲜',
  `price_range` VARCHAR(64) COMMENT '价格区间',
  `where_to_eat` VARCHAR(255) COMMENT '推荐地点',
  `season` VARCHAR(32) COMMENT '最佳季节',
  `tags` JSON COMMENT '标签JSON数组',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美食知识库';

-- =============================
-- 经验补齐知识表
-- =============================
CREATE TABLE IF NOT EXISTS `knowledge_experience` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform` VARCHAR(64) DEFAULT NULL COMMENT '来源平台，如 tavily / dashscope',
  `title` VARCHAR(255) DEFAULT NULL COMMENT '标题',
  `content` MEDIUMTEXT COMMENT '经验正文',
  `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签，逗号分隔',
  `url` VARCHAR(512) DEFAULT NULL COMMENT '原始链接',
  `sync_status` TINYINT NOT NULL DEFAULT 0 COMMENT '同步状态：0未同步，1已同步',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sync_status_update` (`sync_status`, `update_time`),
  KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经验补齐知识库';

-- =============================
-- 幂等处理：先删后插（按名称）
-- =============================
DELETE FROM `knowledge_attraction` WHERE `name` IN (
  '西湖', '灵隐寺', '西溪湿地',
  '拙政园', '平江路',
  '兵马俑', '大雁塔',
  '宽窄巷子', '锦里古街',
  '外滩', '豫园',
  '故宫', '颐和园'
);

DELETE FROM `knowledge_food` WHERE `name` IN (
  '西湖醋鱼', '东坡肉', '龙井虾仁',
  '苏式汤面', '松鼠桂鱼',
  '肉夹馍', '羊肉泡馍',
  '夫妻肺片', '钟水饺',
  '小笼包', '生煎包',
  '北京烤鸭', '炸酱面'
);

-- =============================
-- 插入景点演示数据
-- =============================
INSERT INTO `knowledge_attraction`
(`name`,`description`,`category`,`season`,`suitable_for`,`price_level`,`rating`,`tags`,`address`,`opening_hours`,`ticket_price`,`features`,`is_deleted`)
VALUES
('西湖','杭州核心景区，适合环湖慢游与夜景打卡。','自然','全年','家庭/情侣/年轻人',2,4.9,'["杭州","湖景","城市名片"]','浙江省杭州市西湖风景名胜区','全天开放','免费（部分景点收费）','["苏堤春晓","断桥残雪","环湖骑行"]',0),
('灵隐寺','千年古刹，历史文化氛围浓厚。','文化','全年','家庭/老人/文化爱好者',3,4.8,'["杭州","寺庙","文化"]','浙江省杭州市西湖区灵隐路法云弄1号','07:00-18:00','约45元','["飞来峰","祈福","古建筑"]',0),
('西溪湿地','城市湿地公园，生态环境好，适合半日游。','自然','春/秋','家庭/情侣',3,4.7,'["杭州","湿地","生态"]','浙江省杭州市西湖区天目山路518号','08:00-17:30','约80元','["摇橹船","水上森林","观鸟"]',0),
('拙政园','苏州代表性古典园林，适合文化深度游。','文化','春/秋','家庭/情侣/文化爱好者',3,4.8,'["苏州","园林","世界文化遗产"]','江苏省苏州市姑苏区东北街178号','07:30-17:30','约80元','["江南园林","亭台楼阁","摄影"]',0),
('平江路','苏州古城水巷，夜景与小店体验感强。','休闲','全年','年轻人/情侣',2,4.6,'["苏州","古街","夜游"]','江苏省苏州市姑苏区平江路','全天开放','免费','["小桥流水","文艺店铺","夜景散步"]',0),
('兵马俑','世界级考古遗址，历史震撼感强。','文化','全年','家庭/文化爱好者',4,4.9,'["西安","历史","博物馆"]','陕西省西安市临潼区秦陵北路','08:30-18:00','约120元','["秦代历史","大型遗址","讲解服务"]',0),
('大雁塔','西安地标建筑，适合夜景和历史文化游。','文化','全年','家庭/情侣',3,4.7,'["西安","地标","唐文化"]','陕西省西安市雁塔区雁塔路南段11号','08:00-22:00','登塔约30元','["音乐喷泉","夜景灯光","唐风街区"]',0),
('宽窄巷子','成都人文休闲街区，适合美食与慢生活体验。','休闲','全年','年轻人/家庭',2,4.5,'["成都","街区","美食"]','四川省成都市青羊区金河路口','全天开放','免费','["川西建筑","茶馆体验","伴手礼"]',0),
('锦里古街','成都热门古街，夜间氛围浓厚。','休闲','全年','情侣/年轻人',2,4.5,'["成都","古街","夜市"]','四川省成都市武侯区武侯祠大街231号附近','全天开放','免费','["夜景","小吃密集","民俗演出"]',0),
('外滩','上海经典地标，适合夜景与城市风光。','休闲','全年','情侣/年轻人/家庭',2,4.8,'["上海","地标","夜景"]','上海市黄浦区中山东一路','全天开放','免费','["万国建筑群","浦江夜景","步行观景"]',0),
('豫园','上海历史园林，适合老城厢文化体验。','文化','全年','家庭/文化爱好者',3,4.6,'["上海","园林","老城厢"]','上海市黄浦区福佑路168号','09:00-16:30','约40元','["江南园林","城隍庙商圈","传统建筑"]',0),
('故宫','北京核心历史景区，文化信息量大。','文化','全年','家庭/文化爱好者',4,4.9,'["北京","博物院","历史"]','北京市东城区景山前街4号','08:30-17:00','旺季约60元','["皇家建筑群","中轴线","珍贵文物"]',0),
('颐和园','北京大型皇家园林，适合步行与乘船。','自然','春/秋','家庭/老人',3,4.8,'["北京","园林","皇家"]','北京市海淀区新建宫门路19号','06:30-18:00','约30元','["昆明湖","长廊","皇家园林"]',0);

-- =============================
-- 插入美食演示数据
-- =============================
INSERT INTO `knowledge_food`
(`name`,`description`,`category`,`taste`,`price_range`,`where_to_eat`,`season`,`tags`,`is_deleted`)
VALUES
('西湖醋鱼','杭州代表性传统名菜，鱼肉鲜嫩，酸甜平衡。','主食','酸/鲜','50-120元','杭州楼外楼、知味观等杭帮菜馆','全年','["杭州","杭帮菜","经典"]',0),
('东坡肉','肥而不腻，入口即化，适合搭配米饭。','主食','咸/鲜','40-100元','杭州新白鹿、外婆家等杭帮餐厅','全年','["杭州","杭帮菜","红烧"]',0),
('龙井虾仁','清鲜爽口，茶香与虾仁风味结合。','主食','鲜','60-160元','杭州湖滨商圈、河坊街周边餐厅','春/夏','["杭州","茶香","清淡"]',0),
('苏式汤面','讲究汤底与浇头，口感清爽。','主食','咸/鲜','15-40元','苏州观前街、十全街面馆','全年','["苏州","面食","本地早餐"]',0),
('松鼠桂鱼','苏帮菜经典，外酥里嫩，酸甜开胃。','主食','甜/酸','80-180元','苏州老字号苏帮菜馆','全年','["苏州","苏帮菜","宴请"]',0),
('肉夹馍','西安地道小吃，外酥内香。','小吃','咸/鲜','8-20元','西安回民街、钟楼周边小吃店','全年','["西安","小吃","便捷"]',0),
('羊肉泡馍','陕西特色，汤浓肉香，饱腹感强。','主食','咸/鲜','25-50元','西安回民街、永兴坊','秋/冬','["西安","陕西菜","暖胃"]',0),
('夫妻肺片','成都冷菜代表，麻辣鲜香。','小吃','辣/鲜','20-50元','成都春熙路、锦里周边川菜馆','全年','["成都","川菜","麻辣"]',0),
('钟水饺','甜辣微麻，风味鲜明。','小吃','辣/甜','15-35元','成都建设路、宽窄巷子附近小吃店','全年','["成都","小吃","川味"]',0),
('小笼包','上海经典点心，汤汁丰富。','小吃','鲜','12-40元','上海城隍庙、豫园周边点心店','全年','["上海","点心","早餐"]',0),
('生煎包','底脆汁多，口感层次丰富。','小吃','咸/鲜','8-25元','上海黄河路、南京西路附近小店','全年','["上海","街头小吃","热门"]',0),
('北京烤鸭','北京代表性名菜，皮脆肉嫩。','主食','咸/鲜','120-300元','北京全聚德、便宜坊等老字号','全年','["北京","名菜","宴请"]',0),
('炸酱面','北京家常主食，酱香浓郁。','主食','咸/鲜','20-45元','北京前门、鼓楼周边面馆','全年','["北京","面食","家常"]',0);

SET FOREIGN_KEY_CHECKS = 1;
