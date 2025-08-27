import 'dart:math';

import 'package:hainong/common/ui/button_image_widget.dart';
import 'package:hainong/common/ui/label_custom.dart';
import 'package:hainong/features/function/tool/map_task/components/map_nutrition/map_nutrition_ui_widget.dart';
import 'package:hainong/features/function/tool/map_task/components/map_pet/map_pet_ui_widget.dart';
import 'package:hainong/features/function/tool/map_task/components/map_weather/map_weather_ui_widget.dart';
import 'package:hainong/features/function/tool/map_task/components/tab_body_image_widget.dart';
import 'package:hainong/features/function/tool/map_task/components/tab_body_overview_widget.dart';
import 'package:hainong/features/function/tool/map_task/map_model_update_page.dart';
import 'package:hainong/features/function/tool/map_task/map_pet_filter_page.dart';
import 'package:hainong/features/function/tool/map_task/map_task_bloc.dart';
import 'package:hainong/features/function/tool/map_task/models/map_address_model.dart';
import 'package:hainong/features/function/tool/map_task/models/map_data_model.dart';
import 'package:hainong/features/function/tool/map_task/models/map_deep_link_model.dart';
import 'package:hainong/features/function/tool/map_task/models/map_enum.dart';
import 'package:hainong/features/function/tool/map_task/models/map_response_model.dart';
import 'package:hainong/features/function/tool/map_task/utils/dialog_utils.dart';
import 'package:hainong/features/function/tool/map_task/utils/map_utils.dart';
import 'package:hainong/features/post/ui/import_lib_ui_post.dart';
import 'package:just_audio/just_audio.dart';
import 'package:location/location.dart' as _location;
import 'package:permission_handler/permission_handler.dart';
import 'package:textfield_tags/textfield_tags.dart';
import 'package:trackasia_gl/mapbox_gl.dart';

import '../suggestion_map/UI/utils/trackasia_map_source.dart';
import 'components/disease_color_legend_widget.dart';
import 'components/layer_tree_value_color_widget.dart';
import 'components/layer_tree_value_menu_widget.dart';
import 'components/map_component_widget.dart';
import 'components/map_model/map_model_ui_widget.dart';
import 'components/popup_map_menu_button_widget.dart';
import 'models/map_item_menu_model.dart';

class MapTaskPage extends BasePage {
  MapTaskPage({this.deepLink, this.openPest, Key? key}) : super(pageState: _MapTaskPageState(), key: key);
  final MapDeepLinkModel? deepLink;
  final bool? openPest;
}

class _MapTaskPageState extends BasePageState with TickerProviderStateMixin {
  final sourceIdPetMap = "source_id_pet_map";
  final sourceIdWeatherMap = "source_id_weather_map";
  final sourceIdNutritionMap = "source_id_nutrition_map";
  final sourceIdDemonMap = "source_id_demo_map";
  final sourceIdStoreMap = "source_id_store_map";
  final sourceIdStorageMap = "source_id_storage_map";
  final keyLayerPetMap = "layer_pet_map";
  final keyLayerWeatherMap = "layer_weather_map";
  final keyLayerDemonMap = "layer_demo_map";
  final keyLayerStoreMap = "layer_store_map";
  final keyLayerStorageMap = "layer_storage_map";
  TrackasiaMapController? mapController;
  TrackasiaMapSource clusterSource = TrackasiaMapSource();
  final List<MenuItemMap> _menuList = MapUtils.menuListMap();
  List<MenuItemMap> _treeNutritionalList = [], _treeTypeList = [];
  List<ColorItemMap> _colorTreeValueList = [];
  final ValueNotifier<List<MenuItemMap>> _treeNutritionNotifier = ValueNotifier([]), _treeTypeNotifier = ValueNotifier([]);
  final ValueNotifier<MapAddressModel> _addressMap = ValueNotifier<MapAddressModel>(MapAddressModel());
  final ValueNotifier<bool> _expandedDescription = ValueNotifier(false), _expandedTime = ValueNotifier(false), _isPlay = ValueNotifier(false);
  final ValueNotifier<MapDataModel> _dataMap = ValueNotifier<MapDataModel>(MapDataModel());
  MenuItemMap? _treeType, _treeNutritionType;
  final tagTreeValueController = StringTagController();
  List<MapGeoJsonModel> dataMap = [];
  Color selectedColor = Colors.blue;
  DateTime? _fromDate, _toDate;
  ItemModel _currentProvince = ItemModel();
  List<String> _currentKinds = [];
  MapMenuEnum _selectMenu = MapMenuEnum.none;
  MapModelEnum _selectTabMenuModel = MapModelEnum.demonstration;
  late TabController _tabMenuController, _tabBodyController;
  bool isShowMapDemoBottomSheet = false, isAddLayer = false, isDeepLink = false;
  LatLng? tappedNutritionPosition;
  Symbol? tappedNutritionSymbol;
  int indexPetZoom = 1;
  String imageUser = "", _audio_weather = '';
  MapDeepLinkModel? deepLink;
  MapTaskPage? page;
  AudioPlayer _player = AudioPlayer();
  bool _serviceEnabled = false;
  PermissionStatus _permissionGranted = PermissionStatus.denied;
  MapDataModel dataDialog = MapDataModel();
  final GlobalKey<DiseaseColorLegendWidgetState> _diseaseLegendKey = GlobalKey<DiseaseColorLegendWidgetState>();

  @override
  void dispose() {
    mapController?.dispose();
    _tabMenuController.dispose();
    _tabBodyController.dispose();
    try {
      if (_player.playing) _player.stop();
      _player.dispose();
    } catch (_) {}
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    page = widget as MapTaskPage;
    if (page?.deepLink != null) {
      isDeepLink = true;
      deepLink = page?.deepLink;
      _selectMenu = deepLink!.menu;
      _selectTabMenuModel = deepLink!.tab;
      bloc?.add(ChangeIndexTabEvent());
    }
    _getImageUser();
    bloc = MapTaskBloc();
    bloc!.stream.listen((state) async {
      if (state is GetPetMapState) {
        addPetGeoMap(state.data);
        if (isDeepLink) handleDeepLinkMapModel();
      } else if (state is GetTreeParamsState) {
        setLayerNutrition(state.nutritionType, state.treeType);
      } else if (state is GetWeatherMapState) {
        if (isCheckFeatureGeoData(state.data)) addWeatherGeoMap(state.data);
      } else if (state is GetDemonstrationParadigmsState) {
        if (isCheckFeatureGeoData(state.data)) {
          addModelGeoMap(state.data, MapUtils.getImageNameModelMap(_selectTabMenuModel));
          if (isDeepLink) handleDeepLinkMapModel();
        }
      } else if (state is GetListAgenciesState) {
        if (isCheckFeatureGeoData(state.data)) {
          addModelGeoMap(state.data, MapUtils.getImageNameModelMap(_selectTabMenuModel));
          if (isDeepLink) handleDeepLinkMapModel();
        }
      } else if (state is GetLocationMapState) {
        showNutritionBottomSheet(state.data, state.point);
      } else if (state is GetDetailDemonstrationParadigmState) {
        if (!isShowMapDemoBottomSheet) {
          dataDialog = state.data;
          isShowMapDemoBottomSheet = true;
          _dataMap.value = MapDataModel(has_commented: dataDialog.has_commented, old_rate: dataDialog.old_rate, old_comment: dataDialog.old_comment);
          DialogUtils.showMapBottomSheet(context, topBodyModelWidget(dataDialog, _selectTabMenuModel), tabMenuBodyWidget(context, dataDialog), tabBarBodyWidget(context, dataDialog), height: 0.60.sh)
              .then((value) {
            isShowMapDemoBottomSheet = false;
            _expandedDescription.value = false;
            _expandedTime.value = false;
          });
        }
      } else if (state is GetDetailAgenciesState) {
        if (!isShowMapDemoBottomSheet) {
          dataDialog = state.data;
          isShowMapDemoBottomSheet = true;
          _dataMap.value = MapDataModel(has_commented: dataDialog.has_commented, old_rate: dataDialog.old_rate, old_comment: dataDialog.old_comment);
          DialogUtils.showMapBottomSheet(context, topBodyModelWidget(dataDialog, _selectTabMenuModel), tabMenuBodyWidget(context, dataDialog), tabBarBodyWidget(context, dataDialog), height: 0.60.sh)
              .then((value) {
            isShowMapDemoBottomSheet = false;
            _expandedDescription.value = false;
            _expandedTime.value = false;
          });
        }
      } else if (state is GetLocationState) {
        _addressMap.value = state.response;
      } else if (state is LoadDetailWeatherState) {
        showWeatherBottomSheet(MapGeoJsonModel(state.data), state.point);
      } else if (state is PlayAudioState) {
        _isPlay.value = state.value;
      } else if (state is ShowErrorState) {
        UtilUI.showCustomDialog(context, state.resp);
      } else if (state is GetPetMapDetailState) {
        dataDialog = state.data;
        DialogUtils.showMapBottomSheet(
                context, topBodyPetWidget(dataDialog), tabMenuPetBodyWidget(context, dataDialog), tabBarPetWidget(context, _tabBodyController, dataDialog, imageUser, _expandedDescription),
                height: 0.60.sh)
            .then((value) {
          _expandedDescription.value = false;
        });
      }
    });
    _treeNutritionalList = MapUtils.menuNutritionalTypeModel();
    _treeNutritionNotifier.value = List.from(_treeNutritionalList);
    _tabMenuController = TabController(length: 3, vsync: this);
    _tabBodyController = TabController(length: 2, vsync: this);
    if (page!.openPest == true) selectMenuMap(1);

    _fetchPetMap();
  }

  bool isCheckFeatureGeoData(MapGeoJsonModel data) {
    return data.data["data"]["features"] != null && data.data["data"]["features"].isNotEmpty;
  }

  void _autoPlayAudio(String link) {
    _audio_weather = link;
    _initPlayController();
  }

  void _playPauseAudio() {
    if (_audio_weather.isNotEmpty) {
      _isPlay.value ? _player.play() : _player.pause();
    }
  }

  void _initPlayController() async {
    if (_audio_weather.isEmpty) return;
    _player = AudioPlayer();
    _player.setUrl(_audio_weather).whenComplete(() {
      _player.playerStateStream.listen((playerState) {
        if (playerState.processingState == ProcessingState.completed) {
          _isPlay.value = false;
          _player.seek(const Duration(seconds: 0)).whenComplete(() => _player.pause());
        }
      });
    });
    _player.play();
  }

  void _getImageUser() {
    SharedPreferences.getInstance().then((prefs) {
      final Constants constants = Constants();
      if (prefs.containsKey(constants.image)) imageUser = prefs.getString(constants.image) ?? "";
    });
  }

  void _addCurrentPostion(LatLng point) {
    mapController?.clearSymbols();
    mapController?.addSymbol(SymbolOptions(
      geometry: point,
      iconImage: "assets/images/v9/map/ic_map_location.png",
      iconSize: 1.0,
    ));
  }

  void showNutritionBottomSheet(MapGeoJsonModel data, LatLng point) {
    setNutritionPostionMapClick(point);
    bloc?.add(GetLocationEvent(point));
    if (!isShowMapDemoBottomSheet) {
      final isRecommend = isShowLevelTree();
      if (!isRecommend) {
        _tabBodyController = TabController(length: 1, vsync: this);
      } else {
        _tabBodyController = TabController(length: 2, vsync: this);
      }
      if (_selectMenu == MapMenuEnum.nutrition) {
        DialogUtils.showMapInfoBottomSheet(context, addressNutritionWidget(_addressMap, _treeType),
                tabBarNutritionWidget(context, _addressMap, _tabBodyController, _treeType, _treeNutritionType, data, point, isRecommend: isShowLevelTree()))
            .then((value) {
          isShowMapDemoBottomSheet = false;
        });
      }
    }
  }

  void showWeatherBottomSheet(MapGeoJsonModel data, LatLng point) {
    if (!isShowMapDemoBottomSheet) {
      try {
        DialogUtils.showMapInfoBottomSheet(
                context,
                addressWeatherWidget(_addressMap),
                tabBodyOverviewWeatherWidget(context, data, point, _isPlay, (isRun, url) {
                  if (url.isNotEmpty) {
                    if (isRun) {
                      _isPlay.value = isRun;
                      _autoPlayAudio(url);
                    } else {
                      _isPlay.value = isRun;
                      _playPauseAudio();
                    }
                  } else {
                    UtilUI.showCustomDialog(context, 'Không lấy được thông tin thời tiết');
                  }
                }),
                height: 0.56.sh)
            .whenComplete(() {
          _isPlay.value = false;
          isShowMapDemoBottomSheet = false;
          _playPauseAudio();
        });
      } catch (e) {
        logDebug(e);
      }
    }
  }

  Future<void> _onMapCreated(TrackasiaMapController initialMap) async {
    mapController = initialMap;
    mapController?.requestMyLocationLatLng();
    _initFeatureTapped();
    _markerListerner();
    if (deepLink != null) {
      switch (deepLink!.menu) {
        case MapMenuEnum.model:
          switch (deepLink!.tab) {
            case MapModelEnum.demonstration:
              _selectTabMenuModel = MapModelEnum.demonstration;
              _tabMenuController.animateTo(0);
              bloc?.add(ChangeIndexTabEvent());
              return bloc?.add(GetDemonstrationParadigmsEvent(sourceIdDemonMap, keyLayerDemonMap));
            case MapModelEnum.store:
              _selectTabMenuModel = MapModelEnum.store;
              _tabMenuController.animateTo(1);
              bloc?.add(ChangeIndexTabEvent());
              return bloc?.add(GetListAgenciesEvent('shop', sourceIdStoreMap, keyLayerStoreMap));
            case MapModelEnum.storage:
              _selectTabMenuModel = MapModelEnum.storage;
              _tabMenuController.animateTo(2);
              bloc?.add(ChangeIndexTabEvent());
              return bloc?.add(GetListAgenciesEvent('warehouse', sourceIdStorageMap, keyLayerStorageMap));
          }
        case MapMenuEnum.pet:
          return _prefetchPetMapData(isSave: true);
        default:
      }
    }
  }

  handleDeepLinkMapModel() {
    if (deepLink!.menu == MapMenuEnum.pet) {
      mapController?.animateCamera(CameraUpdate.newLatLngZoom(LatLng(double.parse(deepLink!.lat), double.parse(deepLink!.lng)), 10));
      bloc?.add(GetPetMapDetailEvent(int.parse(deepLink!.id), deepLink!.classable_type));
    } else {
      switch (_selectTabMenuModel) {
        case MapModelEnum.store:
          mapController?.animateCamera(CameraUpdate.newLatLngZoom(LatLng(double.parse(deepLink!.lat), double.parse(deepLink!.lng)), 10));
          bloc?.add(GetDetailAgenciesEvent(int.parse(deepLink!.id)));
          break;
        case MapModelEnum.storage:
          mapController?.animateCamera(CameraUpdate.newLatLngZoom(LatLng(double.parse(deepLink!.lat), double.parse(deepLink!.lng)), 10));
          bloc?.add(GetDetailAgenciesEvent(int.parse(deepLink!.id)));
          break;
        case MapModelEnum.demonstration:
          mapController?.animateCamera(CameraUpdate.newLatLngZoom(LatLng(double.parse(deepLink!.lat), double.parse(deepLink!.lng)), 10));
          bloc?.add(GetDetailDemonstrationParadigmEvent((int.parse(deepLink!.id))));
          break;
        default:
      }
    }
  }

  void _initFeatureTapped() {
    mapController?.onFeatureTapped.add(
      (id, point, coordinates) async {
        if (_selectMenu == MapMenuEnum.pet) {
          var features = await getFeatureData(point, ["${keyLayerPetMap}_pet_circle_children"]);
          if (features?.isNotEmpty ?? false) {
            showMapPetBottomSheet(features!, point, coordinates);
          } else {
            if (indexPetZoom <= 2) {
              mapController!.animateCamera(CameraUpdate.newLatLngZoom(coordinates, indexPetZoom * 6));
              indexPetZoom += 1;
              return;
            }
          }
        }
        if (_selectMenu == MapMenuEnum.nutrition) {
          bloc?.add(GetLocationMapEvent(_treeNutritionType!.value, _treeType?.value ?? "", coordinates));
          return;
        }
        if (_selectMenu == MapMenuEnum.weather) {
          if (Platform.isAndroid) {
            var features = await getFeatureData(point, ["${keyLayerWeatherMap}_image"]);
            if (features?.isNotEmpty ?? false) {
              final data = MapUtils.handleFeaturesData(features!, isPoint: true);
              if (data != null) {
                bloc?.add(LoadDetailWeatherEvent(LatLng(data.lat, data.lng), data.id.toString()));
              }
            }
            return;
          }
        }
        handleMapModelDetailSelect(point);
      },
    );
  }

  void handleMapModelDetailSelect(Point<double> point) async {
    if (_selectMenu == MapMenuEnum.model) {
      switch (_selectTabMenuModel) {
        case MapModelEnum.demonstration:
          var features = await getFeatureData(point, ["${keyLayerDemonMap}_image"]);
          if (features?.isNotEmpty ?? false) {
            final data = MapUtils.handleFeaturesData(features!, isPoint: true);
            if (data != null) bloc?.add(GetDetailDemonstrationParadigmEvent(data.id));
          }
          return;
        case MapModelEnum.store:
          var features = await getFeatureData(point, ["${keyLayerStoreMap}_image"]);
          if (features?.isNotEmpty ?? false) {
            final data = MapUtils.handleFeaturesData(features!, isPoint: true);
            if (data != null) bloc?.add(GetDetailAgenciesEvent(data.id));
          }
          return;
        case MapModelEnum.storage:
          var features = await getFeatureData(point, ["${keyLayerStorageMap}_image"]);
          if (features?.isNotEmpty ?? false) {
            final data = MapUtils.handleFeaturesData(features!, isPoint: true);
            if (data != null) bloc?.add(GetDetailAgenciesEvent(data.id));
          }
          return;
        default:
      }
    }
  }

  void _markerListerner() {
    mapController?.onSymbolTapped.add((argument) {
      final data = argument.data;
    });
  }

  Widget tabMenuBodyWidget(BuildContext root, MapDataModel data) {
    return Container(
      padding: EdgeInsets.symmetric(vertical: 20.sp),
      height: 120.h,
      child: ListView.builder(
        shrinkWrap: true,
        scrollDirection: Axis.horizontal,
        itemCount: 3,
        itemBuilder: (context, index) {
          return GestureDetector(
              onTap: () {
                switch (index) {
                  case 0:
                    MapUtils.openMapApp(const LatLng(10.9514, 107.0855), LatLng(data.lat, data.lng));
                    break;
                  case 1:
                    UtilUI.shareDeeplinkTo(root, data.deep_link, 'Option Share Dialog -> Choose "Share"', 'map task');
                    break;
                  case 2:
                    UtilUI.goToNextPage(root, MapModelUpdatePage(data.classable_id, data.classable_type, title: data.name), funCallback: _reloadComment);
                    break;
                  default:
                }
              },
              child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: Image.asset(
                    MapUtils.menuListModel()[index].image,
                    width: ((1.sw / 3) - 20.sp),
                  )));
        },
      ),
    );
  }

  void _reloadComment(dynamic value) {
    if (value != null && value is Map) {
      _dataMap.value = MapDataModel(has_commented: true, old_rate: value['rate'], old_comment: value['comment']);
    }
  }

  @override
  Widget build(BuildContext context, {Color color = Colors.white}) {
    super.build(context, color: color);
    return Scaffold(
      appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_ios),
            onPressed: () async {
              UtilUI.showCustomDialog(
                context,
                'Nhấn "Đồng Ý" để thoát khỏi bản đồ nông nghiệp',
                title: "Thông báo",
                alignMessageText: TextAlign.center,
                isActionCancel: true,
                isClose: true,
                lblOK: "Đồng ý",
                lblCancel: "Không",
              ).then((value) {
                if (value == true) Navigator.of(context).pop();
              });
            },
          ),
          titleSpacing: 0,
          title: UtilUI.createLabel(MapUtils.nameOptionMap(_selectMenu)),
          centerTitle: true),
      body: Stack(children: [
        TrackasiaMap(
          minMaxZoomPreference: const MinMaxZoomPreference(1, 24),
          styleString: constants.styleMap,
          zoomGesturesEnabled: true,
          compassEnabled: false,
          myLocationEnabled: Platform.isIOS ? true : false,
          initialCameraPosition: const CameraPosition(target: LatLng(15.7146441, 106.401633), zoom: 4.8),
          onMapCreated: _onMapCreated,
          onStyleLoadedCallback: () async {
            if (mapController != null) {
              await TrackasiaMapSource.loadIcons(mapController!);
              await TrackasiaMapSource.loadDonutChartIcons(mapController!);
            }
          },
          onMapClick: _onMapClick,
        ),
        if (_selectMenu == MapMenuEnum.nutrition) layerTreeValueWidget(),
        if (_selectMenu == MapMenuEnum.pet) Align(alignment: Alignment.topLeft, child: DiseaseColorLegendWidget(key: _diseaseLegendKey)),
        treeTypeIconWidget(),
        Loading(bloc),
        if (_selectMenu == MapMenuEnum.model) taskBarMapModelWidget(),
        popupMapMenuWidget()
      ]),
    );
  }

  Widget taskBarMapModelWidget() {
    return Align(
      alignment: Alignment.topCenter,
      child: BlocBuilder(
        bloc: bloc,
        buildWhen: (oldS, newS) => newS is ChangeIndexTabState,
        builder: (BuildContext context, state) {
          return Container(
            height: 62,
            margin: const EdgeInsets.only(top: 10),
            color: Colors.transparent,
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            child: Container(
              color: Colors.transparent,
              child: TabBar(
                padding: EdgeInsets.zero,
                isScrollable: true,
                controller: _tabMenuController,
                labelColor: Colors.white,
                indicatorColor: Colors.black,
                unselectedLabelColor: Colors.black,
                indicatorPadding: EdgeInsets.zero,
                indicator: BoxDecoration(borderRadius: BorderRadius.circular(20), color: const Color(0xFFF39C12)),
                onTap: (value) {
                  removeModelSource(isRemoveAll: false);
                  _selectTabMenuModel = MapUtils.indexEnumModelMap(value + 1);
                  bloc?.add(ChangeIndexTabEvent());
                  if (_selectTabMenuModel == MapModelEnum.demonstration) {
                    bloc?.add(GetDemonstrationParadigmsEvent(sourceIdDemonMap, keyLayerDemonMap));
                  } else if (_selectTabMenuModel == MapModelEnum.store) {
                    bloc?.add(GetListAgenciesEvent('shop', sourceIdStoreMap, keyLayerStoreMap));
                  } else if (_selectTabMenuModel == MapModelEnum.storage) {
                    bloc?.add(GetListAgenciesEvent('warehouse', sourceIdStorageMap, keyLayerStorageMap));
                  }
                },
                tabs: [
                  tabIconWidget("Mô hình trình diễn", image: "ic_map_model_demo.png", backGroundColor: _selectTabMenuModel == MapModelEnum.demonstration ? Colors.transparent : Colors.white),
                  tabIconWidget("Cửa hàng", image: "ic_map_model_store.png", backGroundColor: _selectTabMenuModel == MapModelEnum.store ? Colors.transparent : Colors.white),
                  tabIconWidget("Kho hàng", image: "ic_map_model_warehouse.png", backGroundColor: _selectTabMenuModel == MapModelEnum.storage ? Colors.transparent : Colors.white)
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget popupMapMenuWidget() {
    return Align(
      alignment: Alignment.topRight,
      child: Container(
        margin: const EdgeInsets.all(6),
        padding: EdgeInsets.all(24.sp),
        child: Column(children: [
          PopupMapMenuButton(selectMenuId: _selectMenu, menuList: _menuList, onCallBack: (value) => selectMenuMap(value)),
          if (isShowMenuModelOption()) ...[
            SizedBox(height: 20.sp),
            GestureDetector(
              onTap: () => selectOptionMap(),
              child: Container(
                  decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.5), spreadRadius: 1, blurRadius: 5, offset: const Offset(0, 1))]),
                  padding: const EdgeInsets.all(12),
                  child: Icon(MapUtils.iconOptionMap(_selectMenu), color: Colors.black)),
            ),
          ]
        ]),
      ),
    );
  }

  Widget layerTreeValueWidget() {
    return isShowMenuLayerOption()
        ? Stack(children: [
            Align(alignment: Alignment.topLeft, child: LayerTreeValueColorWidget(_colorTreeValueList, _treeNutritionType)),
            if (isShowLevelTree())
              Align(
                  alignment: Alignment.bottomLeft,
                  child: LayerTreeLayerMenuWidget(
                      selectTreeLayer: _treeNutritionType, menuList: MapUtils.menuNutritionalTypeBottomModel(), onCallBack: (value) => _setTreeNutritionType(value, isUpdate: true)))
          ])
        : Container();
  }

  Widget treeTypeIconWidget() {
    return Align(
        alignment: Alignment.bottomRight,
        child: SafeArea(
          child: Padding(
            padding: EdgeInsets.only(bottom: 100.sp, right: 30.sp),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                GestureDetector(
                  onTap: () {
                    MapUtils.getCurrentPositionMap().then((value) {
                      mapController?.animateCamera(CameraUpdate.newLatLng(LatLng(value.latitude, value.longitude)));
                    });
                  },
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.5), spreadRadius: 1, blurRadius: 5, offset: const Offset(0, 1))],
                    ),
                    padding: const EdgeInsets.all(12),
                    child: const Icon(Icons.near_me_outlined, color: Colors.black),
                  ),
                ),
                SizedBox(height: 40.sp),
                _treeType != null
                    ? GestureDetector(
                        onTap: () => selectOptionMap(),
                        child: ButtonImageWidget(
                            30.sp,
                            () {},
                            Padding(
                                padding: EdgeInsets.all(20.sp),
                                child: Image.asset(
                                  _treeType!.image,
                                  width: 86.w,
                                  height: 86.w,
                                )),
                            color: Colors.white,
                            elevation: 4.0),
                      )
                    : const SizedBox(),
              ],
            ),
          ),
        ));
  }

  void selectOptionMap() {
    switch (_selectMenu) {
      case MapMenuEnum.pet:
        _showPetMapFilter(context);
        break;
      case MapMenuEnum.nutrition:
        _showMapFarmingOption();
        break;
      case MapMenuEnum.weather:
        break;
      case MapMenuEnum.model:
        break;
      default:
    }
  }

  void selectMenuMap(value) {
    _setDefault();
    _closeLegendIfVisible();
    switch (value) {
      case 1:
        _selectMenu = MapMenuEnum.pet;
        _prefetchPetMapData(isSave: false);
        break;
      case 2:
        _selectMenu = MapMenuEnum.nutrition;
        addSourceNutrition();
        _showMapFarmingOption();
        break;
      case 3:
        _selectMenu = MapMenuEnum.weather;
        _fetchWeatherMap();
        break;
      case 4:
        _selectMenu = MapMenuEnum.model;
        selectMenuMapModel(_selectTabMenuModel);
        break;
      default:
    }
    setState(() {});
  }

  void selectMenuMapModel(value) {
    switch (value) {
      case MapModelEnum.demonstration:
        _fetchDemonstrationParadigmMap();
        break;
      case MapModelEnum.store:
        _fetchStoreMap();
        break;
      case MapModelEnum.storage:
        _fetchStogradeMap();
        break;
      default:
    }
  }

  void _showMapFarmingOption() {
    DialogUtils.showMapFarmingBottomSheet(
        context,
        ValueListenableBuilder<List<MenuItemMap>>(
            valueListenable: _treeNutritionNotifier,
            builder: (context, treeNutritionList, child) {
              if (treeNutritionList.isNotEmpty) {
                return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  _title("Kiểu dinh dưỡng"),
                  SizedBox(height: 20.sp),
                  Wrap(
                      children: _getTreeItems(treeNutritionList, _treeNutritionType?.id, (item) {
                    _treeType = null;
                    _setTreeNutritionType(item);
                    if (item.value == "ph" || item.value == "ec" || item.value == "om") {
                      _treeType = null;
                      _treeTypeList.clear();
                      Navigator.of(context).pop();
                    }
                  }))
                ]);
              }
              return Container();
            }),
        ValueListenableBuilder<List<MenuItemMap>>(
            valueListenable: _treeTypeNotifier,
            builder: (context, treeTypeList, child) {
              if (treeTypeList.isNotEmpty) {
                return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  _title("Cây lương thực"),
                  SizedBox(height: 20.sp),
                  _mapItem(MapUtils.menuTreeRiceItem(), _treeType?.id, (item) {
                    Navigator.of(context).pop();
                    _setTreeType(item);
                  }),
                  _title("Cây ăn quả"),
                  SizedBox(height: 20.sp),
                  Wrap(
                      children: _getTreeItems(treeTypeList, _treeType?.id, (item) {
                    Navigator.of(context).pop();
                    _setTreeType(item);
                  })),
                ]);
              }
              return Container();
            }));
  }

  Widget _title(String title) => LabelCustom(title, color: Colors.black87, size: 42.sp, weight: FontWeight.bold);

  Widget tabBarBodyWidget(BuildContext root, MapDataModel data) {
    return Container(
      color: Colors.transparent,
      child: Column(
        children: [
          TabBar(
              padding: EdgeInsets.zero,
              isScrollable: true,
              controller: _tabBodyController,
              labelColor: Colors.blue,
              indicatorColor: Colors.blue,
              unselectedLabelColor: Colors.black,
              indicatorWeight: 1,
              labelPadding: EdgeInsets.symmetric(horizontal: 200.sp),
              indicatorSize: TabBarIndicatorSize.tab,
              indicatorPadding: EdgeInsets.zero,
              unselectedLabelStyle: const TextStyle(color: Colors.black87),
              indicator: UnderlineTabIndicator(borderSide: const BorderSide(width: 4.0, color: Colors.blue), insets: EdgeInsets.symmetric(horizontal: 40.sp)),
              tabs: const [Tab(text: "Tổng quan"), Tab(text: "Hình ảnh")]),
          Expanded(
            child: TabBarView(
                controller: _tabBodyController,
                children: [tabBodyOverviewWidget(root, _selectTabMenuModel, data, _expandedTime, _expandedDescription, imageUser, _reloadComment, _dataMap), tabBodyImageWidget(data)]),
          ),
        ],
      ),
    );
  }

  void _onMapClick(Point<double> point, LatLng coordinates) async {
    if (_selectMenu == MapMenuEnum.pet) {
      var features = await getFeatureData(point, ["${keyLayerPetMap}_pet_circle_children"]);
      if (features?.isNotEmpty ?? false) {
        if (Platform.isIOS) showMapPetBottomSheet(features!, point, coordinates);
      }
      return;
    }
    if (_selectMenu == MapMenuEnum.weather) {
      var features = await getFeatureData(point, ["${keyLayerWeatherMap}_image"]);
      if (features?.isNotEmpty ?? false) {
        final data = MapUtils.handleFeaturesData(features!, isPoint: true);
        if (data != null) {
          bloc?.add(LoadDetailWeatherEvent(LatLng(data.lat, data.lng), data.id.toString()));
        }
      }
      return;
    }
    if (_selectMenu == MapMenuEnum.nutrition) {
      bloc!.add(GetLocationMapEvent(_treeNutritionType!.value, _treeType?.value ?? "", coordinates));
      return;
    }
    handleMapModelDetailSelect(point);
  }

  Future<List?> getFeatureData(Point<double> point, List<String> keys) async {
    final allLayers = await mapController?.getLayerIds() ?? [];
    List<String> matchingLayers = [];
    for (var key in keys) {
      for (var layerId in allLayers) {
        if (layerId is String && layerId.startsWith(key)) {
          matchingLayers.add(layerId);
        }
      }
    }
    if (matchingLayers.isNotEmpty) return await mapController?.queryRenderedFeatures(point, matchingLayers, null);
    return await mapController?.queryRenderedFeatures(point, keys, null);
  }

  void showMapPetBottomSheet(List<dynamic> features, Point<double> point, LatLng coordinates) async {
    try {
      final data = MapUtils.handleFeaturesData(features);
      if (data != null && data.classable_type.isNotEmpty && data.classable_id != 0) {
        bloc?.add(GetPetMapDetailEvent(data.classable_id, data.classable_type));
      }
    } catch (e) {
      logDebug(e.toString());
    }
  }

  Future<void> setNutritionPostionMapClick(LatLng coordinates) async {
    if (_selectMenu == MapMenuEnum.nutrition) {
      tappedNutritionPosition = coordinates;
      if (tappedNutritionSymbol != null) mapController!.removeSymbol(tappedNutritionSymbol!);
      final symbol = await mapController!.addSymbol(SymbolOptions(geometry: coordinates, iconImage: "assets/images/v9/map/ic_map_location.png", iconSize: 0.6));
      setState(() => tappedNutritionSymbol = symbol);
    }
  }

  void _setTreeNutritionType(value, {bool isUpdate = false}) {
    if (value != null && value.id != _treeNutritionType?.id) {
      _treeTypeList = MapUtils.menuTreeTypeModel(value.value);
      _treeTypeNotifier.value = List.from(_treeTypeList);
      _treeNutritionType = value;
      if (!isShowLevelTree()) loadTreeType();
      if (isUpdate) loadTreeType();
      _treeNutritionNotifier.notifyListeners();
      _treeTypeNotifier.notifyListeners();
    }
    setState(() {});
  }

  void _setTreeType(value) {
    if (value != null && value.id != _treeType?.id) {
      bloc!.add(GetTreeParamsEvent(_treeNutritionType?.value ?? "", value.value));
      _treeType = value;
      _colorTreeValueList = MapUtils.menuLayerColor(_treeNutritionType!.value, _treeType?.value);
      _treeTypeNotifier.notifyListeners();
    }
    setState(() {});
  }

  void loadTreeType() {
    _colorTreeValueList = MapUtils.menuLayerColor(_treeNutritionType!.value, _treeType?.value);
    bloc!.add(GetTreeParamsEvent(_treeNutritionType?.value ?? "", _treeType?.value ?? ""));
  }

  void _setDefault() {
    mapController?.invalidateAmbientCache();
    _setSymbolsClear();
    _setTreeClear();
    removeNutritionSource();
    removePetSource();
    removeWeatherSource();
    removeModelSource();
    _ensurePetLayerRemoved();
  }

  void _setSymbolsClear() => mapController?.clearSymbols();

  void _setTreeClear() {
    _treeNutritionType = null;
    _treeType = null;
    _treeTypeList.clear();
  }

  Widget? prefixIcon(InputFieldValues<String> inputFieldValues) {
    return inputFieldValues.tags.isNotEmpty
        ? SingleChildScrollView(
            controller: inputFieldValues.tagScrollController,
            scrollDirection: Axis.vertical,
            child: Padding(
              padding: const EdgeInsets.only(top: 8, bottom: 8, left: 8),
              child: Wrap(
                  runSpacing: 4.0,
                  spacing: 0,
                  children: inputFieldValues.tags.map((String tag) {
                    return Container(
                      decoration: const BoxDecoration(
                        borderRadius: BorderRadius.all(Radius.circular(20.0)),
                        color: Color.fromARGB(255, 74, 137, 92),
                      ),
                      margin: const EdgeInsets.symmetric(horizontal: 2.0),
                      padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 5.0),
                      child: Row(mainAxisAlignment: MainAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [InkWell(child: Text('#$tag', style: const TextStyle(color: Colors.white)))]),
                    );
                  }).toList()),
            ),
          )
        : null;
  }

  void fitBoundsToCoordinates(List<dynamic> bbox) {
    if (bbox.isNotEmpty) {
      final LatLngBounds bounds = LatLngBounds(
        southwest: LatLng(bbox[1], bbox[0]),
        northeast: LatLng(bbox[3], bbox[2]),
      );
      mapController?.animateCamera(CameraUpdate.newLatLngBounds(bounds, left: 50.0, top: 50.0, right: 50.0, bottom: 50.0));
    }
  }

  void cleanTreeValue() => tagTreeValueController.clearTags();

  String petParams() {
    String params = '';
    if (_fromDate != null) params = 'from_date=${MapUtils.datetimeToFormat2(_fromDate)}&';
    if (_toDate != null) params += 'to_date=${MapUtils.datetimeToFormat2(_toDate)}&';
    if (_currentProvince.id.isNotEmpty && _currentProvince.id != "-1") params += 'province_id=${_currentProvince.id}&';
    if (_currentKinds.isNotEmpty) {
      params += 'diagnostic_ids=[$_currentKinds]';
    } else if (params.isNotEmpty) {
      params = params.substring(0, params.length - 1);
    }
    return params;
  }

  void _prefetchPetMapData({bool isSave = false}) {
    String params = petParams();
    bloc?.add(GetPetMapEvent(sourceIdPetMap, params, isSave: isSave));
  }

  void _fetchPetMap() => bloc?.add(GetPetMapEvent(sourceIdPetMap, '', isSave: true));

  void _fetchDemonstrationParadigmMap() => bloc?.add(GetDemonstrationParadigmsEvent(sourceIdDemonMap, keyLayerDemonMap));

  void _fetchStoreMap() => bloc?.add(GetListAgenciesEvent('shop', sourceIdStoreMap, keyLayerStoreMap));

  void _fetchStogradeMap() => bloc?.add(GetListAgenciesEvent('warehouse', sourceIdStorageMap, keyLayerStorageMap));

  void _fetchWeatherMap() => bloc?.add(GetWeatherMapEvent(sourceIdWeatherMap));

  void addPetGeoMap(MapGeoJsonModel item) {
    if (item.data.isEmpty || mapController == null) return;
    clusterSource.addPetClusterV2Map(context: context, mapController: mapController, dataMap: item.data, sourceId: sourceIdPetMap, keyChartName: keyLayerPetMap);
  }

  void addWeatherGeoMap(MapGeoJsonModel item) {
    clusterSource.addWeatherClusterMap(mapController: mapController, dataMap: item.data['data'], sourceId: sourceIdWeatherMap, keyChartName: keyLayerWeatherMap);
  }

  void addModelGeoMap(MapGeoJsonModel item, String image) =>
      clusterSource.addModelClusterMap(mapController: mapController, model: _selectTabMenuModel, dataMap: item.data['data'], sourceId: item.sourceId!, keyChartName: item.layerId!, image: image);

  void addSourceNutrition() => TrackasiaMapSource.addSourceNutritionMap(mapController, sourceIdNutritionMap);

  void setLayerNutrition(String nutritionType, String treeType) => TrackasiaMapSource.setLayerNutritionMap(mapController, sourceIdNutritionMap, nutritionType, treeType);

  void removeNutritionSource() => TrackasiaMapSource.removeAllLayerNutritionMap(mapController);

  void removeWeatherSource() => TrackasiaMapSource.removeSourceWeatherMap(mapController, sourceIdWeatherMap, keyLayerWeatherMap);

  void removePetSource() {
    try {
      TrackasiaMapSource.removeSourcePetMap(mapController, sourceIdPetMap, keyLayerPetMap);
      _closeLegendIfVisible();
    } catch (e) {
      print("Error in removePetSource: $e");
    }
  }

  Future<void> _ensurePetLayerRemoved() async {
    try {
      if (mapController != null) {
        final layers = await mapController!.getLayerIds();
        for (var layerId in layers) {
          if (layerId.contains(keyLayerPetMap)) {
            await mapController!.removeLayer(layerId);
          }
        }
        await mapController!.removeSource(sourceIdPetMap);
        await mapController!.clearSymbols();
      }
    } catch (e) {
      logDebug("Error removing pet map layers: $e");
    }
  }

  void removeModelSource({bool isRemoveAll = true}) {
    if (isRemoveAll) {
      TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdDemonMap, keyLayerDemonMap);
      TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdStoreMap, keyLayerStoreMap);
      TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdStorageMap, keyLayerStorageMap);
    } else {
      switch (_selectTabMenuModel) {
        case MapModelEnum.demonstration:
          return TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdDemonMap, keyLayerDemonMap);
        case MapModelEnum.store:
          return TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdStoreMap, keyLayerStoreMap);
        case MapModelEnum.storage:
          return TrackasiaMapSource.removeSourceModelMap(mapController, sourceIdStorageMap, keyLayerStorageMap);
        default:
      }
    }
  }

  void _showPetMapFilter(BuildContext context) async {
    final result = await Navigator.push(context, MaterialPageRoute(builder: (context) => MapPetFilterPage(_currentKinds, _currentProvince, _fromDate, _toDate)));
    Util.trackActivities('', path: 'Pests Map -> Open Filter View');
    if (result != null) {
      _currentKinds = result['petIds'] ?? [];
      _currentProvince = result['province'] ?? ItemModel();
      _fromDate = result['from_date'];
      _toDate = result['to_date'];
      _prefetchPetMapData(isSave: true);
    }
  }

  List<Widget> _getTreeItems(List<MenuItemMap> list, int? selectId, Function(MenuItemMap) callbackItem) {
    final List<Widget> temp = [];
    for (var element in list) {
      temp.add(_mapItem(element, selectId, (item) => callbackItem(item)));
    }
    return temp;
  }

  Widget _mapItem(MenuItemMap item, int? selectId, Function(MenuItemMap) callbackItem) {
    return Container(
      margin: EdgeInsets.all(17.sp),
      decoration: BoxDecoration(borderRadius: BorderRadius.circular(32.sp)),
      child: Column(children: [
        ButtonImageWidget(32.sp, () {
          callbackItem(item);
        },
            Container(
                decoration: BoxDecoration(borderRadius: BorderRadius.circular(32.sp), border: Border.all(width: 4, color: item.id == selectId ? Colors.blueAccent : Colors.transparent)),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(22.sp),
                  child: Image.asset(
                    item.image,
                    width: 188.w,
                    height: 188.w,
                  ),
                )),
            color: Colors.white,
            elevation: 0.0),
        SizedBox(height: 20.sp),
        SizedBox(
          width: 0.15.sw,
          child: Text(item.name, textAlign: TextAlign.center, maxLines: 2, style: const TextStyle(color: Colors.black87, fontSize: 14)),
        )
      ]),
    );
  }

  bool isShowMenuModelOption() => _selectMenu != MapMenuEnum.model && _selectMenu != MapMenuEnum.none && _selectMenu != MapMenuEnum.weather;

  bool isShowMenuLayerOption() => _treeNutritionType != null && _treeNutritionalList.isNotEmpty;

  bool isShowLevelTree() => _treeNutritionType?.value != "ph" && _treeNutritionType?.value != "ec" && _treeNutritionType?.value != "om";

  Future<void> _checkLocationPermission() async {
    _permissionGranted = await Permission.location.status;
    if (_permissionGranted != PermissionStatus.granted) {
      _permissionGranted = await Permission.location.request();
      if (_permissionGranted != PermissionStatus.granted) {
        setState(() => _serviceEnabled = false);
        UtilUI.showCustomDialog(context, 'Bạn chưa bật dịch vụ định vị');
        return;
      }
    }
    _location.Location location = _location.Location();
    _serviceEnabled = await location.serviceEnabled();
    if (!_serviceEnabled) {
      _serviceEnabled = await location.requestService();
      if (!_serviceEnabled) {
        setState(() => _serviceEnabled = false);
        return;
      }
    }
    setState(() => _serviceEnabled = true);
  }

  void _closeLegendIfVisible() {
    final legendWidgetState = _diseaseLegendKey.currentState;
    if (legendWidgetState != null && legendWidgetState.isExpanded) {
      legendWidgetState.toggleExpanded();
    }
  }
}
