import 'dart:io';
import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter/material.dart';
import 'package:hainong/common/constants.dart';
import 'package:hainong/features/function/tool/map_task/utils/map_utils.dart';
import 'package:hainong/features/function/tool/suggestion_map/UI/utils/trackasia_util.dart';
import 'package:trackasia_gl/mapbox_gl.dart';

import '../../../map_task/models/map_enum.dart';

class TrackasiaMapSource {
  static const Duration _layerDelay = Duration(milliseconds: 50);

  //================MAP CHART LAYER==============//
  Future<void>? addTrackasiaClusterMap({required TrackasiaMapController? mapController, required String sourceId, required Map<String, dynamic> dataMap, required String keyChartName}) async {
    final keyChartImageCircleRate = keyChartName + "_chart_image_circle_rate";
    final keyChartCircleRate = keyChartName + "_chart_circle_rate";
    final keyChartChildren = keyChartName + "_chart_circle_children";
    final keyChartCircleCount = keyChartName + "_chart_circle_count";
    if (dataMap.isNotEmpty) {
      dataMap["type"] = "FeatureCollection";
      await addClusteredPointSource(mapController: mapController, sourceId: sourceId, data: dataMap);
      await addClusteredPointLayers(
          mapController: mapController,
          sourceId: sourceId,
          keyChartImageCircleRate: keyChartImageCircleRate,
          keyChartCircleRate: keyChartCircleRate,
          keyChartChildren: keyChartChildren,
          keyChartCircleCount: keyChartCircleCount);
    }
  }

  Future<void>? addPetClusterMap(
      {required BuildContext context, required TrackasiaMapController? mapController, required String sourceId, required Map<String, dynamic> dataMap, required String keyChartName}) async {
    final keyChartImageCircleRate = keyChartName + "_chart_image_circle_rate";
    final keyChartCircleRate = keyChartName + "_chart_circle_rate";
    final keyChartChildren = keyChartName + "_chart_circle_children";
    final keyPetChildren = keyChartName + "_pet_circle_children";
    final keyChartCircleCount = keyChartName + "_chart_circle_count";
    if (dataMap.isNotEmpty) {
      dataMap["type"] = "FeatureCollection";
      await addClusteredPointSource(mapController: mapController, sourceId: sourceId, data: dataMap);
      await addPetClusteredPointLayers(
          mapController: mapController,
          sourceId: sourceId,
          keyChartImageCircleRate: keyChartImageCircleRate,
          keyChartCircleRate: keyChartCircleRate,
          keyChartChildren: keyChartChildren,
          keyPetChildren: keyPetChildren,
          keyChartCircleCount: keyChartCircleCount);
    }
  }

  //================MAP CHART LAYER V2==============//
  Future<void> addPetClusterV2Map({
    required BuildContext? context,
    required TrackasiaMapController? mapController,
    required Map<String, dynamic> dataMap,
    required String sourceId,
    required String keyChartName,
  }) async {
    if (mapController == null || context == null || dataMap.isEmpty) return;
    dataMap["type"] = "FeatureCollection";
    await addClusteredPointSourceChunked(mapController: mapController, sourceId: sourceId, data: dataMap, keyChartName: keyChartName);
  }

  Future<void>? addWeatherClusterMap({required TrackasiaMapController? mapController, required String sourceId, required Map<String, dynamic> dataMap, required String keyChartName}) async {
    final keyImage = keyChartName + "_image";
    final keyCircle = keyChartName + "_circle";
    final keyCircleCount = keyChartName + "_circle_count";
    final keyCircleColor = keyChartName + "_circle_color";
    if (dataMap.isNotEmpty) {
      await addClusteredPointSource(mapController: mapController, sourceId: sourceId, data: dataMap, maxZoom: 11);
      await addClusteredWeatherLayers(
        mapController: mapController,
        sourceId: sourceId,
        keyChartImage: keyImage,
        keyChartCircle: keyCircle,
        keyChartCircleCount: keyCircleCount,
        keyCircleColor: keyCircleColor,
      );
    }
  }

  Future<void>? addModelClusterMap(
      {required TrackasiaMapController? mapController,
      required MapModelEnum model,
      required String sourceId,
      required Map<String, dynamic> dataMap,
      required String keyChartName,
      required String image}) async {
    final keyImage = keyChartName + "_image";
    final keyText = keyChartName + "_text";
    final keyCircle = keyChartName + "_circle";
    final keyCircleCount = keyChartName + "_circle_count";
    if (dataMap.isNotEmpty) {
      await addClusteredPointSource(mapController: mapController, sourceId: sourceId, data: dataMap, maxZoom: 10);
      await addClusteredModelLayers(
          mapController: mapController, model: model, sourceId: sourceId, keyImage: keyImage, keyText: keyText, keyCircle: keyCircle, keyCircleCount: keyCircleCount, image: image);
    }
  }

  Future<void>? addClusteredPointSource({required TrackasiaMapController? mapController, required String sourceId, required Map<String, dynamic>? data, double? maxZoom}) async {
    if (mapController == null || data == null) return;
    final sourceIds = await mapController.getSourceIds();
    final exists = sourceIds.contains(sourceId);
    if (exists) {
      await mapController.setGeoJsonSource(sourceId, data);
    } else {
      await mapController.addSource(sourceId, GeojsonSourceProperties(data: data, cluster: true, clusterMaxZoom: maxZoom ?? 10));
    }
  }

  Future<void> addClusteredPointLayers(
      {required TrackasiaMapController? mapController,
      required String sourceId,
      required String keyChartImageCircleRate,
      required String keyChartCircleRate,
      required String keyChartChildren,
      required String keyChartCircleCount}) async {
    await addImageCircleRate(mapController: mapController, sourceId: sourceId, keyLayer: keyChartImageCircleRate);
    await addChartCircleRate(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircleRate, keyImage: keyChartImageCircleRate);
    await addChartChildren(mapController: mapController, sourceId: sourceId, keyLayer: keyChartChildren);
    await addCircleCount(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircleCount);
  }

  Future<void> addPetClusteredPointLayers(
      {required TrackasiaMapController? mapController,
      required String sourceId,
      required String keyChartImageCircleRate,
      required String keyChartCircleRate,
      required String keyChartChildren,
      required String keyPetChildren,
      required String keyChartCircleCount}) async {
    await addImageCircleRate(mapController: mapController, sourceId: sourceId, keyLayer: keyChartImageCircleRate);
    await addChartCircleRate(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircleRate, keyImage: keyChartImageCircleRate);
    // await addChartChildren(mapController: mapController, sourceId: sourceId, keyLayer: keyChartChildren);
    await addPetCircleColor(mapController: mapController, sourceId: sourceId, keyLayer: keyChartChildren);
    await addIconPetChildren(mapController: mapController, sourceId: sourceId, keyLayer: keyPetChildren);
    await addCircleCount(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircleCount);
  }

  Future<void> addClusteredWeatherLayers(
      {required TrackasiaMapController? mapController,
      required String sourceId,
      required String keyChartImage,
      required String keyChartCircle,
      required String keyCircleColor,
      required String keyChartCircleCount}) async {
    // await addWeatherColor(mapController: mapController, sourceId: sourceId, keyLayer: keyCircleColor);
    await addWeatherCircle(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircle, keyImage: keyChartCircle);
    await addImageWeather(mapController: mapController, sourceId: sourceId, keyLayer: keyChartImage);
    // await addWeatherCount(mapController: mapController, sourceId: sourceId, keyLayer: keyChartCircleCount);
  }

  Future<void> addClusteredModelLayers(
      {required TrackasiaMapController? mapController,
      required MapModelEnum model,
      required String sourceId,
      required String keyImage,
      required String keyText,
      required String keyCircle,
      required String keyCircleCount,
      required String image}) async {
    // await addModelCircle(mapController: mapController, model: model, sourceId: sourceId, keyLayer: keyCircle, keyImage: keyCircle);
    await addImageModel(mapController: mapController, sourceId: sourceId, keyLayer: keyImage, image: image);
    await addModelCount(mapController: mapController, sourceId: sourceId, keyLayer: keyCircleCount);
  }

  //================MAP CHART LAYER==============//

  //================MAP CHART ADD==============//

  Future<void> addImageCircleRate({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    try {
      if (mapController == null) return;
      await removeLayer(mapController: mapController, keyLayer: keyLayer);
      await mapController.addLayer(
        sourceId,
        keyLayer,
        const SymbolLayerProperties(
          iconImage: [
            'case',
            [
              '>=',
              ['get', 'point_count'],
              10000
            ],
            'cluster_10000',
            [
              '>=',
              ['get', 'point_count'],
              5000
            ],
            'cluster_5000',
            [
              '>=',
              ['get', 'point_count'],
              2500
            ],
            'cluster_2500',
            [
              '>=',
              ['get', 'point_count'],
              2000
            ],
            'cluster_2000',
            [
              '>=',
              ['get', 'point_count'],
              1000
            ],
            'cluster_1000',
            [
              '>=',
              ['get', 'point_count'],
              500
            ],
            'cluster_500',
            [
              '>=',
              ['get', 'point_count'],
              200
            ],
            'cluster_200',
            [
              '>=',
              ['get', 'point_count'],
              100
            ],
            'cluster_100',
            [
              '>=',
              ['get', 'point_count'],
              50
            ],
            'cluster_50',
            [
              '>=',
              ['get', 'point_count'],
              10
            ],
            'cluster_10',
            'cluster_1'
          ],
          iconSize: [
            'interpolate',
            ['linear'],
            ['get', 'point_count'],
            1, 1.0, // 1 điểm: kích thước 1.0
            10, 1.2, // 10 điểm: kích thước 1.2
            50, 1.3, // 50 điểm: kích thước 1.3
            100, 1.4, // 100 điểm: kích thước 1.4
            500, 1.6, // 500 điểm: kích thước 1.6
            1000, 1.8, // 1000 điểm: kích thước 1.8
            5000, 2.0, // 5000 điểm: kích thước 2.0
            10000, 2.2 // 10000 điểm trở lên: kích thước 2.2
          ],
          iconAllowOverlap: true,
          symbolSortKey: 10, // Đặt cao hơn để hiển thị trên các layer khác
          iconIgnorePlacement: true,
        ),
        // Thêm filter để chỉ áp dụng cho các điểm có thuộc tính point_count
        filter: [Expressions.has, 'point_count'],
      );

      // Kiểm tra xem layer đã được thêm thành công chưa
      final layerIds = await mapController.getLayerIds();
      if (layerIds.contains(keyLayer)) {
        logDebug("===SUCCESSFULLY ADDED IMAGE CIRCLE RATE LAYER: $keyLayer===");
      } else {
        logDebug("===WARNING: Layer $keyLayer was not added successfully===");
      }
    } catch (e, stackTrace) {
      logDebug("===ERROR ADDING IMAGE CIRCLE RATE LAYER: $keyLayer===");
      logDebug("Error details: $e");
      logDebug("Stack trace: $stackTrace");
    }
  }

  Future<void> addImageWeather({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    logDebug("===ADD LAYER ===>$keyLayer");
    await mapController!.addLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          iconImage: ['get', 'icon'],
          iconSize: Platform.isIOS ? 0.8 : 0.4,
          iconAllowOverlap: true,
        ));
  }

  Future<void> addImageModel({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, required String image}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    logDebug("===ADD LAYER ===>$keyLayer");
    await mapController?.addLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          iconImage: image,
          iconSize: Platform.isIOS ? 1.6 : 0.9,
          iconAllowOverlap: true,
        ));
  }

  Future<void> addTextModel({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    logDebug("===ADD LAYER ===>$keyLayer");
    await mapController?.addLayer(sourceId, keyLayer, const SymbolLayerProperties());
  }

  Future<void> addChartCircleRate({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, required String keyImage}) async {
    const pointKey = "point_count";

    try {
      await removeLayer(mapController: mapController, keyLayer: keyLayer);
      logDebug("===ADDING CHART CIRCLE RATE LAYER: $keyLayer===");

      await mapController?.addSymbolLayer(
          sourceId,
          keyLayer,
          SymbolLayerProperties(
            textHaloWidth: 1,
            textSize: 6,
            iconImage: keyImage,
            // Điều chỉnh kích thước icon dựa trên số lượng điểm
            iconSize: [
              'interpolate',
              ['linear'],
              ['get', pointKey],
              // [số điểm, kích thước]
              1, 0.7, // Ít nhất 1 điểm, kích thước 0.7
              10, 0.8, // 10 điểm, kích thước 0.8
              50, 0.9, // 50 điểm, kích thước 0.9
              100, 1.0, // 100 điểm, kích thước 1.0
              200, 1.1, // 200 điểm, kích thước 1.1
              500, 1.2, // 500 điểm, kích thước 1.2
              1000, 1.3, // 1000 điểm, kích thước 1.3
              2000, 1.4, // 2000 điểm, kích thước 1.4
              5000, 1.5, // 5000 điểm, kích thước 1.5
              10000, 1.6 // 10000 điểm trở lên, kích thước 1.6
            ],
            iconAllowOverlap: true,
            symbolSortKey: 1,
            iconIgnorePlacement: true,
          ),
          filter: [Expressions.has, pointKey]);

      logDebug("===SUCCESSFULLY ADDED CHART CIRCLE RATE LAYER: $keyLayer===");
    } catch (e) {
      logDebug("===ERROR ADDING CHART CIRCLE RATE LAYER: $e===");
      logDebug("Error details: $e");
    }
  }

  Future<void> addPetCircleRate({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, required String keyImage}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addSymbolLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          textHaloWidth: 1,
          textSize: 6,
          iconImage: keyImage,
          iconSize: [
            'step',
            ['zoom'],
            1, // Zoom level 0: Size 1
            0.9, // Zoom level 1: Size 0.9
            200, // Zoom level 2: Size 200
            1.1, // Zoom level 3: Size 1.1
            400, // Zoom level 4: Size 400
            1.2, // Zoom level 5: Size 1.2
            800, // Zoom level 6: Size 800
            1.3, // Zoom level 7: Size 1.3
            1000, // Zoom level 8: Size 1000
            1.4 // Zoom level 9 and above: Size 1.4
          ],
          iconAllowOverlap: true,
        ),
        filter: [Expressions.has, "point_count"]);
  }

  Future<void> addWeatherCircle({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, required String keyImage}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    logDebug("===ADD LAYER ===>$keyLayer");
    await mapController!.addLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          iconImage: [
            'step',
            ['get', 'point_count'],
            '01dd',
            1000,
            '01dd',
            5000,
            '01dd',
          ],
          iconSize: Platform.isIOS ? 0.6 : 0.32,
          iconAllowOverlap: true,
        ),
        filter: [Expressions.has, "point_count"]);
  }

  Future<void> addModelCircle({required TrackasiaMapController? mapController, required MapModelEnum model, required String sourceId, required String keyLayer, required String keyImage}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController!.addLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          iconImage: [
            'step',
            ['get', 'point_count'],
            model == MapModelEnum.demonstration
                ? 'ic_map_model_marker_store_demo'
                : model == MapModelEnum.store
                    ? 'ic_map_model_marker_store'
                    : 'ic_map_model_marker_storage',
            10,
            model == MapModelEnum.demonstration
                ? 'ic_map_model_marker_store_demo'
                : model == MapModelEnum.store
                    ? 'ic_map_store_medium'
                    : 'ic_map_storage_medium',
            100,
            model == MapModelEnum.demonstration
                ? 'ic_map_model_marker_store_demo'
                : model == MapModelEnum.store
                    ? 'ic_map_store_large'
                    : 'ic_map_storage_large',
          ],
          iconSize: Platform.isIOS ? 1.2 : 0.8,
          iconAllowOverlap: true,
        ),
        filter: [Expressions.has, "point_count"]);
  }

  Future<void> addChangeChartCircleRate({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, required String keyImage, required String suggest}) async {
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addSymbolLayer(
        sourceId,
        keyLayer,
        SymbolLayerProperties(
          iconImage: keyImage,
          iconAllowOverlap: true,
        ),
        filter: [
          '==',
          ['get', 'suggest'],
          suggest,
        ]);
  }

  Future<void> addCircleCount({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer, int? chunkIndex}) async {
    const pointKey = "point_count";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addSymbolLayer(sourceId, keyLayer, const SymbolLayerProperties(textField: pointKey, textSize: 12), filter: [Expressions.has, pointKey]);
  }

  Future<void> addWeatherCount({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointAbbreviated = "point_count_abbreviated";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addLayer(
        sourceId,
        keyLayer,
        const SymbolLayerProperties(
            textField: [Expressions.get, pointAbbreviated],
            textSize: 12.0,
            textColor: '#000000',
            // textHaloColor: '#00FF00',
            textHaloWidth: 10.0,
            textPadding: 2.0,
            textHaloBlur: 1.0,
            textAllowOverlap: true,
            textIgnorePlacement: true));
  }

  Future<void> addModelCount({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointAbbreviated = "point_count_abbreviated";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    logDebug("===ADD LAYER ===>$keyLayer");
    await mapController?.addLayer(
        sourceId,
        keyLayer,
        const SymbolLayerProperties(
            textField: [Expressions.get, pointAbbreviated],
            textSize: 12.0,
            textColor: '#000000',
            textHaloColor: '#00FF00',
            textHaloWidth: 10.0,
            textPadding: 2.0,
            textHaloBlur: 1.0,
            textAllowOverlap: true,
            textIgnorePlacement: true));
  }

  Future<void> addChartChildren({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addCircleLayer(
        sourceId,
        keyLayer,
        const CircleLayerProperties(circleColor: [
          'case',
          ['has', 'mag'],
          ['get', 'mag'],
          '#00CC33'
        ], circleRadius: 10),
        filter: [
          "!",
          [Expressions.has, pointKey]
        ]);
  }

  Future<void> addPetCircleColor({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    try {
      await removeLayer(mapController: mapController, keyLayer: keyLayer);

      logDebug("===ADDING PET CIRCLE COLOR LAYER: $keyLayer===");

      await mapController?.addCircleLayer(
          sourceId,
          keyLayer,
          const CircleLayerProperties(circleStrokeColor: [
            'case',
            ['has', 'mag'],
            ['get', 'mag'],
            '#00CC33'
          ], circleColor: 'rgba(0, 0, 0, 0)', circleStrokeWidth: 2, circleRadius: 16),
          filter: [
            "!",
            [Expressions.has, pointKey]
          ]);
    } catch (e) {
      logDebug("===ERROR ADDING PET CIRCLE COLOR LAYER: $e===");
    }
  }

  Future<void> addWeatherColor({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addCircleLayer(
        sourceId,
        keyLayer,
        const CircleLayerProperties(
          circleStrokeColor: 'rgba(255, 165, 0, 0.5)',
          circleColor: 'rgba(0, 0, 0, 0)',
          circleStrokeWidth: 2,
          circleRadius: 17,
        ),
        filter: [
          "!",
          [Expressions.has, pointKey]
        ]);
  }

  Future<void> addPetColor({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    await removeLayer(mapController: mapController, keyLayer: keyLayer);
    await mapController?.addCircleLayer(
      sourceId,
      keyLayer,
      const CircleLayerProperties(
        circleStrokeColor: 'rgba(255, 165, 0, 0.5)',
        circleColor: 'rgba(0, 0, 0, 0)',
        circleStrokeWidth: 2,
        circleRadius: 17,
      ),
      // filter: [
      //   "!",
      //   [Expressions.has, pointKey]
      // ]
    );
  }

  Future<void> addIconPetChildren({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    try {
      await removeLayer(mapController: mapController, keyLayer: keyLayer);
      logDebug("===ADDING PET CHILDREN LAYER: $keyLayer===");
      // Thêm layer với filter đơn giản để giảm tải xử lý
      await mapController?.addLayer(
          sourceId,
          keyLayer,
          SymbolLayerProperties(
            iconImage: ['get', 'category_id'],
            iconSize: Platform.isIOS ? 0.8 : 0.8,
            iconAllowOverlap: true,
          ),
          filter: [
            "!",
            [Expressions.has, pointKey]
          ]);
    } catch (e) {
      logDebug("===ERROR ADDING PET CHILDREN LAYER: $e===");
    }
  }

  static Future<void> removeSource({required TrackasiaMapController? mapController, required String sourceId}) async {
    final sourceIds = await mapController?.getSourceIds();
    if (sourceIds?.contains(sourceId) == true) {
      await mapController?.removeSource(sourceId);
      logDebug("===REMOVE SOURCE ID===>$sourceId");
    }
  }

  static Future<void> hiddenLayer({required TrackasiaMapController? mapController, List<dynamic>? layerIds}) async {
    List<dynamic>? _layerIds = layerIds;
    _layerIds ??= await mapController?.getLayerIds();
    for (var layer in layers) {
      mapController?.setLayerVisibility(layer, false);
      logDebug("===HIDDEN LAYER ID===>$layer");
    }
  }

  static Future<void> removeLayer({required TrackasiaMapController? mapController, required String keyLayer, List<dynamic>? layerIds}) async {
    List<dynamic>? _layerIds = layerIds;
    _layerIds ??= await mapController?.getLayerIds();
    if (_layerIds?.contains(keyLayer) == true) {
      await mapController?.removeLayer(keyLayer);
      logDebug("===REMOVE LAYER ID===>$keyLayer");
    }
  }

  Future<void> addChangeChartCircleData({required TrackasiaMapController? mapController, required String sourceId, required List<dynamic> dataMap}) async {
    if (dataMap.isNotEmpty == true) {
      for (Map<String, dynamic> feature in dataMap) {
        var rnd = Random();
        String id = feature['properties']['id'] ?? rnd.nextInt(1000);
        String keyLayer = createKeyLayer(id);
      }
    }
  }

  String createKeyLayer(String id) => 'pet_chart_keylayer_circle_rate$id';

  String createImageId(String suggest) => 'pet_chart_image_circle_rate$suggest';

  //================MAP CHART ADD==============//

  String? selectedOption;
  String? selectedCrop;

  static final layers = [
    "lua-n-layer",
    "lua-p-layer",
    "lua-k-layer",
    "lua-npk-layer",
    "cay-an-trai-n-layer",
    "cay-an-trai-p-layer",
    "cay-an-trai-k-layer",
    "cay-an-trai-npk-layer",
    "ec-layer",
    "ph-layer",
    "om-layer",
  ];

  static void addSourceNutritionMap(TrackasiaMapController? mapController, String sourceIdNutritionMap) async {
    mapController?.getSourceIds().then((value) {
      if (!value.contains(sourceIdNutritionMap)) {
        return mapController.addSource(sourceIdNutritionMap, VectorSourceProperties(url: "${Constants().mapUrl}/api/soil_geos/tile_conf.json"));
      }
    });
  }

  static void setLayerNutritionMap(TrackasiaMapController? mapController, String sourceIdNutritionMap, String nutritionType, String treeType) async {
    await removeAllLayerNutritionMap(mapController);
    switch (nutritionType) {
      case "ec":
        await addECLayers(mapController, sourceIdNutritionMap);
        break;
      case "ph":
        await addPHLayers(mapController, sourceIdNutritionMap);
        break;
      case "om":
        await addOMLayers(mapController, sourceIdNutritionMap);
        break;
      case "npk":
        if (treeType == "lua") {
          await addNLayers(mapController, sourceIdNutritionMap);
          await addPLayers(mapController, sourceIdNutritionMap);
          await addKLayers(mapController, sourceIdNutritionMap);
        } else {
          await addFruitKLayers(mapController, sourceIdNutritionMap);
          await addFruitPLayers(mapController, sourceIdNutritionMap);
          await addFruitNLayers(mapController, sourceIdNutritionMap);
        }
        break;
      case "n":
        if (treeType == "lua") {
          await addNLayers(mapController, sourceIdNutritionMap);
        } else {
          await addFruitNLayers(mapController, sourceIdNutritionMap);
        }
        break;
      case "p":
        if (treeType == "lua") {
          await addPLayers(mapController, sourceIdNutritionMap);
        } else {
          await addFruitPLayers(mapController, sourceIdNutritionMap);
        }
        break;
      case "k":
        if (treeType == "lua") {
          await addKLayers(mapController, sourceIdNutritionMap);
        } else {
          await addFruitKLayers(mapController, sourceIdNutritionMap);
        }
        break;
    }
  }

  static Future<void> removeAllLayerNutritionMap(TrackasiaMapController? mapController) async {
    await mapController?.getLayerIds().then((value) async {
      for (var layer in layers) {
        if (value.contains(layer)) {
          await removeLayer(layerIds: value, mapController: mapController, keyLayer: layer);
        }
      }
    });
  }

  static Future<void> removeSourcePetMap(TrackasiaMapController? mapController, String sourceId, String baseLayerId) async {
    if (mapController == null) return;
    try {
      final layerIds = await mapController.getLayerIds();
      for (var layerId in layerIds) {
        if (layerId.startsWith("disease_layer_") || layerId.contains(baseLayerId)) {
          try {
            await mapController.removeLayer(layerId);
            logDebug("Removed layer: $layerId");
          } catch (e) {
            logDebug("Error removing layer $layerId: $e");
          }
        }
      }
      try {
        await mapController.removeSource(sourceId);
        logDebug("Removed source: $sourceId");
      } catch (e) {
        logDebug("Error removing source $sourceId: $e");
      }
    } catch (e) {
      logDebug("Error in removeSourcePetMap: $e");
    }
  }

  static void removeSourceWeatherMap(TrackasiaMapController? mapController, String sourceId, String layerId) {
    removeSource(mapController: mapController, sourceId: sourceId);
    mapController?.getLayerIds().then((value) {
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_image");
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_circle");
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_circle_count");
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_circle_color");
    });
  }

  static void removeSourceModelMap(TrackasiaMapController? mapController, String sourceId, String layerId) {
    removeSource(mapController: mapController, sourceId: sourceId);
    mapController?.getLayerIds().then((value) {
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_image");
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_circle");
      removeLayer(layerIds: value, mapController: mapController, keyLayer: "${layerId}_circle_count");
    });
  }

  static Future<void> addLayerWithParams(TrackasiaMapController? mapController, String sourceId, String layerId, List<dynamic>? filter, List<dynamic> fillColor) async {
    await addLayer(mapController: mapController, sourceId: sourceId, layerId: layerId, filter: filter, fillColor: fillColor);
  }

  static Future<void> addNLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'lua-n-layer', [
      '==',
      ['get', 'layer'],
      'lua'
    ], [
      "match",
      ["get", "n"],
      ["< 0,091", "< 0.091"],
      "#22FA7F",
      ["0,091-0,18", "0.091-0.18"],
      "#59D400",
      ["0,181-0,27", "0.181-0.27"],
      "#6AFF00",
      ["0,271-0,36", "0.271-0.36"],
      "#00FF00",
      ["0,361-0,45", "0.361-0.45"],
      "#FF00FF",
      ["> 0,45"],
      "#FEF6CE",
      "transparent"
    ]);
  }

  static Future<void> addPLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'lua-p-layer', [
      '==',
      ['get', 'layer'],
      'lua'
    ], [
      "match",
      ["get", "p"],
      ["< 3,76"],
      "#2F73aa",
      ["3,76-7,5", "< 5.25"],
      "#59D400",
      ["7,6-11,25", "5.25-10.5"],
      "#6AFF00",
      ["11,26-15,0", "10.51-15.7"],
      "#00FF00",
      ["15,1-18,75", "15.76-21"],
      "#F0A0E2",
      ["> 18,75", "21.1-26.25"],
      "#FF00FF",
      ["> 26.25"],
      "#FEF6CE",
      "transparent"
    ]);
  }

  static Future<void> addKLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'lua-k-layer', [
      '==',
      ['get', 'layer'],
      'lua'
    ], [
      "match",
      ["get", "k"],
      ["0,076-0,15", "0.091-0.18"],
      "#59D400",
      ["0,151-0,225", "0.181-0.27"],
      "#6AFF00",
      ["0,226-0,3", "0.271-0.36"],
      "#00FF00",
      ["0,31-0,375", "0.361-0.45"],
      "#FF00FF",
      ["> 0,375", "> 0.45"],
      "#FEF6CE",
      ["< 1"],
      "#aaabba",
      ["1,0-4,56", "1.0-4.56"],
      "#5AA188",
      ["4,56-6,61", "4.56-6.61"],
      "#AFFF99",
      ["6,61-9,94", "6.61-9.94"],
      "#F0A0a2",
      ["9,94-14,62", "9.94-14.62"],
      "#FE11B8",
      ["> 14,62", "> 14.62"],
      "#1113B8",
      "transparent"
    ]);
  }

  static Future<void> addFruitNLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'cay-an-trai-n-layer', [
      "in",
      ["get", "layer"],
      [
        "literal",
        ["sau_rieng", "xoai", "buoi", "cam", "nhan", "thanh_long"]
      ]
    ], [
      "match",
      ["get", "n"],
      ["< 0,091", "< 0.091"],
      "#22FA7F",
      ["0,091-0,18", "0.091-0.18"],
      "#2F731D",
      ["0,181-0,27", "0.181-0.27"],
      "#5AA132",
      ["0,271-0,36", "0.271-0.36"],
      "#AFFF54",
      ["0,361-0,45", "0.361-0.45"],
      "#F0A0E2",
      ["> 0,45"],
      "#FEE3B8",
      "transparent",
    ]);
  }

  static Future<void> addFruitPLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'cay-an-trai-p-layer', [
      "in",
      ["get", "layer"],
      [
        "literal",
        ["sau_rieng", "xoai", "buoi", "cam", "nhan", "thanh_long"]
      ]
    ], [
      "match",
      ["get", "p"],
      ["< 3,76"],
      "#2F73aa",
      ["3,76-7,5", "< 5.25"],
      "#2F731D",
      ["7,6-11,25", "5.25-10.5"],
      "#5AA132",
      ["11,26-15,0", "10.51-15.7"],
      "#AFFF54",
      ["15,1-18,75", "15.76-21"],
      "#F0A0E2",
      ["> 18,75", "21.1-26.25"],
      "#FEE3B8",
      ["> 26.25"],
      "#F2AC6F",
      "transparent",
    ]);
  }

  static Future<void> addFruitKLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'cay-an-trai-k-layer', [
      "in",
      ["get", "layer"],
      [
        "literal",
        ["sau_rieng", "xoai", "buoi", "cam", "nhan", "thanh_long"]
      ]
    ], [
      "match",
      ["get", "k"],
      ["0,076-0,15", "0.091-0.18"],
      "#2F731D",
      ["0,151-0,225", "0.181-0.27"],
      "#5AA132",
      ["0,226-0,3", "0.271-0.36"],
      "#AFFF54",
      ["0,31-0,375", "0.361-0.45"],
      "#F0A0E2",
      ["> 0,375", "> 0.45"],
      "#FEE3B8",
      ["< 1"],
      "#aaabba",
      ["1,0-4,56", "1.0-4.56"],
      "#5AA188",
      ["4,56-6,61", "4.56-6.61"],
      "#AFFF99",
      ["6,61-9,94", "6.61-9.94"],
      "#F0A0a2",
      ["9,94-14,62", "9.94-14.62"],
      "#FE11B8",
      ["> 14,62", "> 14.62"],
      "#1113B8",
      "transparent"
    ]);
  }

  static Future<void> addPHLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'ph-layer', null, [
      "case",
      [
        "==",
        ["get", "ph"],
        "< 5,0"
      ],
      "#FFFCA8",
      [
        "==",
        ["get", "ph"],
        "5,0-6,0"
      ],
      "#fec300",
      [
        "==",
        ["get", "ph"],
        "6-7,5"
      ],
      "#d4a200",
      "transparent"
    ]);
  }

  static Future<void> addECLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'ec-layer', null, [
      "case",
      [
        "==",
        ["get", "ec"],
        "< 0.8"
      ],
      "#b0fdff",
      [
        "==",
        ["get", "ec"],
        "0,8-1,6"
      ],
      "#3096f8",
      [
        "==",
        ["get", "ec"],
        "> 1.6"
      ],
      "#043e76",
      "transparent"
    ]);
  }

  static Future<void> addOMLayers(TrackasiaMapController? mapController, String sourceId) async {
    await addLayerWithParams(mapController, sourceId, 'om-layer', null, [
      "case",
      [
        "==",
        ["get", "om"],
        "< 4,0"
      ],
      "#b0fd22",
      [
        "==",
        ["get", "om"],
        "4-10"
      ],
      "#309622",
      [
        "==",
        ["get", "om"],
        "> 10"
      ],
      "#043e22",
      "transparent"
    ]);
  }

  static Future<void> addLayer({required mapController, required String sourceId, required String layerId, required List<dynamic>? filter, required List<dynamic> fillColor}) async {
    mapController?.getLayerIds().then((value) async {
      if (!value.contains(layerId)) {
        await mapController.addLayer(sourceId, layerId, FillLayerProperties(fillColor: fillColor, fillOpacity: 0.9), filter: filter, belowLayerId: 'waterway', sourceLayer: "soil");
        logDebug("===ADD LAYER ===>$layerId");
      }
    });
  }

  Future<void> addClusteredPointSourceChunked({
    required TrackasiaMapController? mapController,
    required String sourceId,
    required Map<String, dynamic>? data,
    double? maxZoom,
    required String keyChartName,
  }) async {
    if (mapController == null || data == null || data['features'] == null) return;
    try {
      final features = data['features'] as List;
      if (features.isEmpty) return;
      await _processFeatureChunks(mapController, features, sourceId, keyChartName, maxZoom ?? 10);
    } catch (e) {
      logDebug("Error in chunked processing: $e");
    }
  }

  Future<void> _processFeatureChunks(TrackasiaMapController mapController, List features, String sourceId, String keyChartName, double maxZoom) async {
    final int chunkSize = (features.length / 3).ceil();
    for (var i = 0; i < features.length; i += chunkSize) {
      final end = (i + chunkSize < features.length) ? i + chunkSize : features.length;
      final chunk = features.sublist(i, end);
      final chunkSourceId = "${sourceId}_chunk_${(i ~/ chunkSize) + 1}";
      await _processChunk(mapController, chunk, chunkSourceId, keyChartName, maxZoom, i ~/ chunkSize);
      await Future.delayed(_layerDelay);
    }
  }

  Future<void> _processChunk(TrackasiaMapController mapController, List chunk, String chunkSourceId, String keyChartName, double maxZoom, int chunkIndex) async {
    try {
      Map<String, dynamic> chunkData = {'type': 'FeatureCollection', 'features': chunk};
      await addClusteredPointSource(mapController: mapController, sourceId: chunkSourceId, data: chunkData, maxZoom: maxZoom);
      final chunkSuffix = "_chunk_${chunkIndex + 1}";
      final keyChartImageCircleRate = keyChartName + "_chart_image_circle_rate" + chunkSuffix;
      final keyChartChildren = keyChartName + "_chart_circle_children" + chunkSuffix;
      final keyPetChildren = keyChartName + "_pet_circle_children" + chunkSuffix;
      final keyChartCircleCount = keyChartName + "_chart_circle_count" + chunkSuffix;
      logDebug("===ADDING IMAGE CIRCLE RATE LAYER FIRST FOR CHUNK ${chunkIndex + 1}===");
      await addImageCircleRate(mapController: mapController, sourceId: chunkSourceId, keyLayer: keyChartImageCircleRate);
      await Future.delayed(const Duration(milliseconds: 50));
      await addPetCircleColor(mapController: mapController, sourceId: chunkSourceId, keyLayer: keyChartChildren);
      await Future.delayed(const Duration(milliseconds: 30));
      await addIconPetChildren(mapController: mapController, sourceId: chunkSourceId, keyLayer: keyPetChildren);
      await Future.delayed(const Duration(milliseconds: 30));
      await addCircleCountOptimized(mapController: mapController, sourceId: chunkSourceId, keyLayer: keyChartCircleCount);
      final layerIds = await mapController.getLayerIds();
      logDebug("===LAYER CHECK FOR CHUNK ${chunkIndex + 1}: Circle Rate layer exists: ${layerIds.contains(keyChartImageCircleRate)}===");
    } catch (e) {
      logDebug("===ERROR PROCESSING CHUNK ${chunkIndex + 1}: $e===");
    }
  }

  Future<void> addCircleCountOptimized({required TrackasiaMapController? mapController, required String sourceId, required String keyLayer}) async {
    const pointKey = "point_count";
    try {
      await removeLayer(mapController: mapController, keyLayer: keyLayer);
      const textFieldExpression = [
        "case",
        [
          ">=",
          ["get", pointKey],
          1000
        ],
        [
          "concat",
          [
            "to-string",
            [
              "/",
              [
                "round",
                [
                  "*",
                  [
                    "/",
                    ["get", pointKey],
                    1000
                  ],
                  10
                ]
              ],
              10
            ]
          ],
          "k"
        ],
        [
          "to-string",
          ["get", pointKey]
        ]
      ];

      await mapController?.addSymbolLayer(
          sourceId,
          keyLayer,
          const SymbolLayerProperties(
            textField: textFieldExpression,
            textSize: 12,
            textColor: '#000000',
            textHaloColor: '#FFFFFF',
            textHaloWidth: 1.5,
            textAllowOverlap: true,
            iconAllowOverlap: true,
            symbolSortKey: 10, 
          ),
          filter: [Expressions.has, pointKey]);

      logDebug("===OPTIMIZED CIRCLE COUNT LAYER ADDED: $keyLayer===");
    } catch (e) {
      logDebug("===ERROR ADDING OPTIMIZED CIRCLE COUNT LAYER: $e===");
    }
  }

  Future<void> removeExistingPetLayers({required TrackasiaMapController? mapController, required String keyChartName}) async {
    final layerIds = await mapController?.getLayerIds();
    if (layerIds == null) return;
    final suffixes = ["_chart_image_circle_rate", "_chart_circle_rate", "_chart_circle_children", "_pet_circle_children", "_chart_circle_count"];
    for (var layerId in layerIds) {
      if (layerId is String) {
        for (var suffix in suffixes) {
          if (layerId.contains(suffix) && layerId.contains(keyChartName)) {
            await removeLayer(mapController: mapController, keyLayer: layerId);
            logDebug("===REMOVED EXISTING LAYER: $layerId===");
          }
        }
      }
    }
  }

  static Future<void> loadIcons(TrackasiaMapController mapController) async {
    try {
      for (String path in MapUtils.imageIconMapPaths) {
        final ByteData byteData = await rootBundle.load(path);
        final Uint8List bytes = byteData.buffer.asUint8List();
        final String imageName = path.split('/').last.split('.').first;
        await mapController.addImage(imageName, bytes).then((value) {});
      }
    } catch (e) {
      logDebug("Error loading icons: $e");
    }
  }

  static Future<void> loadDonutChartIcons(TrackasiaMapController mapController) async {
    try {
      final clusterConfig = {
        1: {'size': 42, 'stroke': 4},
        10: {'size': 42, 'stroke': 4},
        50: {'size': 42, 'stroke': 4},
        100: {'size': 42, 'stroke': 6},
        200: {'size': 48, 'stroke': 6},
        500: {'size': 48, 'stroke': 6},
        1000: {'size': 48, 'stroke': 6},
        2000: {'size': 52, 'stroke': 6},
        2500: {'size': 52, 'stroke': 6},
        5000: {'size': 52, 'stroke': 6},
        10000: {'size': 52, 'stroke': 6}
      };
      for (var level in clusterConfig.keys) {
        final config = clusterConfig[level]!;
        final imageBytes = await TrackasiaUtils.createDonutChartPng(level, width: config['size']!.toDouble(), height: config['size']!.toDouble(), strokeWidth: config['stroke']!.toDouble());
        if (imageBytes != null) {
          await mapController.addImage('cluster_$level', imageBytes);
        }
      }
      logDebug("Đã tải xong tất cả biểu tượng cluster");
    } catch (e) {
      logDebug("Lỗi khi tải biểu tượng donut chart: $e");
    }
  }
}
