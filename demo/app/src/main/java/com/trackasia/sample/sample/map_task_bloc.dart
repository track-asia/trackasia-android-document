import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hainong/common/api_client.dart';
import 'package:hainong/common/base_bloc.dart';
import 'package:hainong/common/base_response.dart';
import 'package:hainong/common/constants.dart';
import 'package:hainong/common/models/file_byte.dart';
import 'package:hainong/common/models/item_list_model.dart';
import 'package:hainong/features/function/tool/diagnose_pests/diagnose_pests_repository.dart';
import 'package:hainong/features/function/tool/map_task/models/map_address_model.dart';
import 'package:hainong/features/function/tool/map_task/models/map_data_model.dart';
import 'package:hainong/features/function/tool/map_task/models/map_item_menu_model.dart';
import 'package:hainong/features/function/tool/map_task/utils/map_utils.dart';
import 'package:hainong/features/function/tool/suggestion_map/mappage_repository.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'package:trackasia_gl/mapbox_gl.dart';

import '../map_task/models/map_response_model.dart';
import 'models/map_item_model.dart';

class LoadListNameEvent extends BaseEvent {}

class LoadNutritionalTypeEvent extends BaseEvent {}

class LoadTreeTypeEvent extends BaseEvent {
  final String type;
  LoadTreeTypeEvent(this.type);
}

class GetTreeParamsEvent extends BaseEvent {
  String nutritionType;
  String treeTyle;
  GetTreeParamsEvent(this.nutritionType, this.treeTyle);
}

class PostRatingEvent extends BaseEvent {
  final List<FileByte> list;
  final String content;
  final int rate;
  final int id;
  final String type;
  PostRatingEvent(this.id, this.list, this.content, this.rate, this.type);
}

class PostImageMapEvent extends BaseEvent {
  final List<FileByte> list;
  final String content;
  final int id;
  final String type;
  PostImageMapEvent(this.id, this.content, this.type, this.list);
}

class GetTreeValuesEvent extends BaseEvent {
  GetTreeValuesEvent();
}

class GetPetMapEvent extends BaseEvent {
  String sourceId;
  String params;
  bool? isSave = false;
  GetPetMapEvent(this.sourceId, this.params, {this.isSave = false});
}

class GetWeatherMapEvent extends BaseEvent {
  String sourceId;
  String? params;
  GetWeatherMapEvent(this.sourceId, {this.params});
}

class GetDemonstrationParadigmsEvent extends BaseEvent {
  String sourceId;
  String layerId;
  GetDemonstrationParadigmsEvent(this.sourceId, this.layerId);
}

class GetDetailDemonstrationParadigmEvent extends BaseEvent {
  final int id;
  GetDetailDemonstrationParadigmEvent(this.id);
}

class GetListAgenciesEvent extends BaseEvent {
  final String type;
  final String sourceId;
  final String layerId;
  GetListAgenciesEvent(this.type, this.sourceId, this.layerId);
}

class GetLocationMapEvent extends BaseEvent {
  LatLng point;
  String nutritionType;
  String treeTyle;
  GetLocationMapEvent(this.nutritionType, this.treeTyle, this.point);
}

class GetDetailAgenciesEvent extends BaseEvent {
  final int id;
  GetDetailAgenciesEvent(this.id);
}

class GetReverseGeocodingEvent extends BaseEvent {
  LatLng point;
  GetReverseGeocodingEvent(this.point);
}

class LoadImageMapEvent extends BaseEvent {}

class ChangeRatingStarEvent extends BaseEvent {}

class ShowKeyboardEvent extends BaseEvent {
  final bool value;
  ShowKeyboardEvent(this.value);
}

class GetLocationEvent extends BaseEvent {
  final LatLng point;
  final MapAddressModel? address;
  final MapGeoJsonModel? data;
  GetLocationEvent(this.point, {this.data, this.address});
}

class GetPetMapDetailEvent extends BaseEvent {
  final String type;
  final int id;
  GetPetMapDetailEvent(this.id, this.type);
}

class CreateDiagnostisPestEvent extends BaseEvent {
  final LatLng point;
  final List<FileByte> files;
  final String pest_name, description, tree_name, province_id, district_id, address;
  CreateDiagnostisPestEvent(this.point, this.province_id, this.district_id, this.address, this.tree_name, this.pest_name, this.description, this.files);
}

class LoadCategorysEvent extends BaseEvent {
  String? name;
  LoadCategorysEvent({this.name});
}

class LoadProvincesEvent extends BaseEvent {}

class ChangeProvinceEvent extends BaseEvent {}

class ChangeIndexTabEvent extends BaseEvent {}

class ChangeIndexTabState extends BaseState {}

class SendContributeEvent extends BaseEvent {
  final MapAddressModel address;
  final LatLng point;
  final name, zone_crops, pH, EC, farming_method;
  final List<FileByte> files;
  final List<String> crops;
  SendContributeEvent(this.address, this.point, this.name, this.crops, this.zone_crops, this.pH, this.EC, this.farming_method, this.files);
}

class SendDiseasesPositionEvent extends BaseEvent {
  final String id;
  final List<LatLng> points;
  SendDiseasesPositionEvent(this.id, this.points);
}

class DrawMapEvent extends BaseEvent {}

class LoadDetailWeatherEvent extends BaseEvent {
  final LatLng point;
  final String id;
  LoadDetailWeatherEvent(this.point, this.id);
}

class LoadingAudioEvent extends BaseEvent {
  final bool value;
  LoadingAudioEvent(this.value);
}

class PlayAudioEvent extends BaseEvent {
  final bool value;
  PlayAudioEvent(this.value);
}

class ShowErrorEvent extends BaseEvent {
  String error;
  ShowErrorEvent(this.error);
}

class PlayAudioState extends BaseState {
  final bool value;
  PlayAudioState(this.value);
}

class LoadingAudioState extends BaseState {
  final bool value;
  LoadingAudioState(this.value);
}

class DrawMapState extends BaseState {}

class LoadNutritionalTypeState extends BaseState {
  final List<MenuItemMap> list;
  LoadNutritionalTypeState(this.list);
}

class LoadTreeTypeState extends BaseState {
  final List<MenuItemMap> list;
  LoadTreeTypeState(this.list);
}

class GetTreeParamsState extends BaseState {
  String nutritionType;
  String treeType;
  GetTreeParamsState(this.nutritionType, this.treeType);
}

class GetPetMapState extends BaseState {
  MapGeoJsonModel data;
  GetPetMapState(this.data);
}

class GetWeatherMapState extends BaseState {
  MapGeoJsonModel data;
  GetWeatherMapState(this.data);
}

class GetLocationMapState extends BaseState {
  MapGeoJsonModel data;
  LatLng point;
  GetLocationMapState(this.data, this.point);
}

class LoadImageMapState extends BaseState {}

class ChangeRatingStarState extends BaseState {}

class GetDemonstrationParadigmsState extends BaseState {
  MapGeoJsonModel data;
  GetDemonstrationParadigmsState(this.data);
}

class GetDetailDemonstrationParadigmState extends BaseState {
  final MapDataModel data;
  GetDetailDemonstrationParadigmState(this.data);
}

class GetPetMapDetailState extends BaseState {
  final MapDataModel data;
  GetPetMapDetailState(this.data);
}

class GetTreeValuesState extends BaseState {
  GetTreeValuesState();
}

class GetListAgenciesState extends BaseState {
  final MapGeoJsonModel data;
  GetListAgenciesState(this.data);
}

class GetDetailAgenciesState extends BaseState {
  final MapDataModel data;
  GetDetailAgenciesState(this.data);
}

class GetReverseGeocodingState extends BaseState {
  String address;
  GetReverseGeocodingState(this.address);
}

class ShowKeyboardState extends BaseState {
  final bool value;

  ShowKeyboardState(this.value);
}

class PostRatingState extends BaseState {
  final BaseResponse response;
  PostRatingState(this.response);
}

class GetLocationState extends BaseState {
  final MapAddressModel response;
  final LatLng point;
  final MapAddressModel? address;
  final MapGeoJsonModel? data;
  GetLocationState(this.response, this.point, this.address, this.data);
}

class PostImageMapState extends BaseState {
  final BaseResponse response;
  PostImageMapState(this.response);
}

class CreateDiagnostisPestSuccessState extends BaseState {
  final BaseResponse resp;
  CreateDiagnostisPestSuccessState(this.resp);
}

class LoadCategorysState extends BaseState {
  final List<PetModel> list;
  const LoadCategorysState(this.list);
}

class LoadCategorysFilterState extends BaseState {
  final List<TreeModel> list;
  LoadCategorysFilterState(this.list);
}

class LoadProvincesState extends BaseState {
  final List<ItemModel> data;
  LoadProvincesState(this.data);
}

class ChangeProvinceState extends BaseState {}

class CreateQuestionState extends BaseState {
  final ItemModel data;
  const CreateQuestionState(this.data);
}

class SendDiseasesPositionState extends BaseState {
  final BaseResponse response;
  const SendDiseasesPositionState(this.response);
}

class LoadDetailWeatherState extends BaseState {
  final Map<String, dynamic> data;
  LatLng point;
  LoadDetailWeatherState(this.data, this.point);
}

class MapTaskBloc extends BaseBloc {
  final repository = DiagnosePestsRepository();
  late http.Client client;
  late http.Response response;
  bool _isInitialRequestRunning = false;
  Completer<MapGeoJsonModel>? _initialRequestCompleter;
  bool _isUsingCache = false;

  void cancelCurrentRequest() {
    client.close();
    client = http.Client();
  }

  MapTaskBloc() {
    client = http.Client();
    on<ShowKeyboardEvent>((event, emit) => emit(ShowKeyboardState(event.value)));
    on<GetPetMapEvent>((event, emit) async {
      try {
        if (_isRequestingInitialData(event)) {
          emit(const BaseState(isShowLoading: true));
          return await _waitForInitialRequest(emit);
        }
        if (event.isSave == false) {
          emit(const BaseState(isShowLoading: true));
          final hasCacheData = await _tryLoadFromCache(event, emit);
          if (hasCacheData) {
            _isUsingCache = true;
            emit(const BaseState(isShowLoading: false));
            logDebug("Đã hiển thị dữ liệu từ cache, tiếp tục cập nhật cache ngầm");
          } else {
            logDebug("Không tìm thấy dữ liệu cache, đang đợi dữ liệu từ API...");
          }
        }
        _prepareForApiCall(event);
        response = await client.get(Uri.parse(Constants().baseUrl + Constants().apiVersion + 'diagnostics/pets_map?${event.params}'));
        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          await _savePetMapCache(data);
          if (event.isSave == true) {
            final resultModel = MapGeoJsonModel(data, sourceId: event.sourceId);
            _initialRequestCompleter?.complete(resultModel);
            _isInitialRequestRunning = false;
            logDebug("Đã hoàn thành request ban đầu");
          } else if (!_isUsingCache) {
            emit(GetPetMapState(MapGeoJsonModel(data, sourceId: event.sourceId)));
            emit(const BaseState(isShowLoading: false));
            logDebug("Hiển thị dữ liệu từ API");
          } else {
            logDebug("Không hiển thị dữ liệu API vì đã hiển thị từ cache");
          }
        } else if (event.isSave == false && !_isUsingCache) {
          logDebug("API trả về mã lỗi: ${response.statusCode}");
          emit(const BaseState(isShowLoading: false));
        }
      } catch (e) {
        logDebug("Lỗi khi gọi API bản đồ sâu bệnh: $e");
        if (event.isSave == false && !_isUsingCache) {
          emit(const BaseState(isShowLoading: false));
        }
      } finally {
        if (event.isSave == true || !_isUsingCache) {
          emit(const BaseState(isShowLoading: false));
        }
      }
    });
    on<GetWeatherMapEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      cancelCurrentRequest();
      response = await client.get(Uri.parse(Constants().baseUrl + Constants().apiVersion + 'weather/weather_in_a_week'));
      logDebug("======>${response.body}");
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        emit(GetWeatherMapState(MapGeoJsonModel(data, sourceId: event.sourceId)));
      }
    });
    on<LoadNutritionalTypeEvent>((event, emit) => emit(LoadNutritionalTypeState(MapUtils.menuNutritionalTypeModel())));
    on<LoadTreeTypeEvent>((event, emit) => emit(LoadTreeTypeState(MapUtils.menuTreeTypeModel(event.type))));
    on<LoadImageMapEvent>((event, emit) => emit(LoadImageMapState()));
    on<ChangeRatingStarEvent>((event, emit) => emit(ChangeRatingStarState()));
    on<GetTreeParamsEvent>((event, emit) => emit(GetTreeParamsState(event.nutritionType, event.treeTyle)));
    on<GetLocationMapEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final params = event.nutritionType == 'ec' || event.nutritionType == 'ph' || event.nutritionType == 'om'
          ? 'layer_name=do phi nhieu dat&prop_option=${event.nutritionType}&lat=${event.point.latitude}&lng=${event.point.longitude}'
          : 'layer_name=${event.treeTyle}&prop_option=${event.nutritionType}&lat=${event.point.latitude}&lng=${event.point.longitude}';
      final api = await http.get(Uri.parse('${Constants().mapUrl}/api/soil_geos/location_info?$params'));
      final response = BaseResponse().fromJson(jsonDecode(api.body), MapResponseModel());
      if (response.data != null) {
        final data = MapGeoJsonModel(response.data.data);
        emit(GetLocationMapState(data, event.point));
      } else {
        emit(const BaseState(isShowLoading: false));
      }
    });
    on<GetDemonstrationParadigmsEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      cancelCurrentRequest();
      response = await client.get(Uri.parse(Constants().baseUrl + Constants().apiVersion + 'maps/demonstration_paradigms'));
      logDebug("======>${response.body}");
      emit(const BaseState(isShowLoading: false));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        emit(GetDemonstrationParadigmsState(MapGeoJsonModel(data, sourceId: event.sourceId, layerId: event.layerId)));
      }
    });
    on<GetDetailDemonstrationParadigmEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().getAPI('${Constants().apiVersion}maps/demonstration_paradigms/${event.id}', MapDataModel(), hasHeader: true);
      response.checkOK() ? emit(GetDetailDemonstrationParadigmState(response.data)) : emit(const BaseState());
    });
    on<GetPetMapDetailEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().getAPI('${Constants().apiVersion}training_data/detail?classable_type=${event.type}&classable_id=${event.id}', MapDataModel(), hasHeader: true);
      response.checkOK() ? emit(GetPetMapDetailState(response.data)) : emit(const BaseState());
    });
    on<GetListAgenciesEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      cancelCurrentRequest();
      response = await client.get(Uri.parse(Constants().baseUrl + Constants().apiVersion + 'maps/agencies?agency_type=${event.type}'));
      logDebug("======>${response.body}");
      emit(const BaseState(isShowLoading: false));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        emit(GetListAgenciesState(MapGeoJsonModel(data, sourceId: event.sourceId, layerId: event.layerId)));
      }
    });
    on<GetDetailAgenciesEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().getAPI('${Constants().apiVersion}maps/agencies/${event.id}', MapDataModel(), hasHeader: true);
      response.checkOK() ? emit(GetDetailAgenciesState(response.data)) : emit(const BaseState());
    });
    on<GetReverseGeocodingEvent>((event, emit) async {
      final api = await http.get(Uri.parse('https://map.hainong.vn/api/v1/reverse?lang=vi&point.lon=${event.point.longitude}&point.lat=${event.point.latitude}'));
      final response = jsonDecode(api.body);
      if (response != null) {
        emit(GetReverseGeocodingState(response["features"][0]["properties"]["label"]));
      }
    });
    on<PostRatingEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().postAPI(Constants().apiVersion + "maps/comment_maps", 'POST', BaseResponse(),
          body: {
            "content": event.content,
            "rate": event.rate.toString(),
            "commentable_type": event.type,
            "commentable_id": event.id.toString(),
          },
          realFiles: event.list,
          paramFile: "attachment[file][]");
      emit(PostRatingState(response));
    });
    on<GetLocationEvent>((event, emit) async {
      final response = await ApiClient().getAPI('${Constants().apiVersion}locations/address_full?lat=${event.point.latitude}&lng=${event.point.longitude}', MapAddressModel(), hasHeader: true);
      response.checkOK() ? emit(GetLocationState(response.data, event.point, event.address, event.data)) : emit(const BaseState());
    });
    on<PostImageMapEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().postAPI(Constants().apiVersion + "maps/image_maps", 'POST', BaseResponse(),
          body: {
            "note": event.content,
            "classable_type": event.type,
            "classable_id": event.id.toString(),
          },
          realFiles: event.list,
          paramFile: "attachment[file][]");
      emit(PostImageMapState(response));
    });
    on<CreateDiagnostisPestEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await repository.createDiagnosticContribute(
          event.files, event.province_id, event.district_id, event.address, event.tree_name, event.pest_name, event.description, event.point.latitude.toString(), event.point.longitude.toString());
      emit(CreateDiagnostisPestSuccessState(response));
    });
    on<LoadCategorysEvent>((event, emit) async {
      final resp = await ApiClient().getAPI('${Constants().apiVersion}diagnostics/categories', TreesModel(passUnknown: true), hasHeader: false);
      if (resp.checkOK()) {
        final List<TreeModel> plants = resp.data.list;
        if (event.name?.isNotEmpty == true) {
          final List<PetModel> list = [];
          final TreeModel plant = plants.firstWhere((element) {
            return element.name == event.name;
          }, orElse: () => TreeModel());
          for (var item in plant.diagnostics) {
            list.add(item);
          }
          emit(LoadCategorysState(list));
        } else {
          emit(LoadCategorysFilterState(plants));
        }
      }
    });
    on<LoadProvincesEvent>((event, emit) async {
      final resp = await MapPageRepository().loadProvinces();
      if (resp.checkOK() && resp.data.list.isNotEmpty) {
        emit(LoadProvincesState(resp.data.list));
      }
    });
    on<SendContributeEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await ApiClient().postAPI(Constants().apiVersion + 'info_diseases', 'POST', ItemModel(),
          body: {
            'address': event.name ?? "",
            'province_id': event.address.provinceId.toString(),
            'district_id': event.address.districtId.toString(),
            'ward_id': event.address.wardId.toString(),
            'lat': event.point.latitude.toString(),
            'lng': event.point.longitude.toString(),
            'crops': jsonEncode(event.crops),
            'zone_crops': event.zone_crops,
            'pH': event.pH,
            'EC': event.EC,
            'farming_method': event.farming_method
          },
          realFiles: event.files,
          paramFile: "attachment[file][]");
      if (response.checkOK(passString: true)) {
        emit(CreateQuestionState(response.data));
      } else {
        emit(ShowErrorState(response.data));
      }
    });
    on<SendDiseasesPositionEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final body = {'info_disease_id': event.id.toString()};
      int i = 0;
      for (var point in event.points) {
        body.putIfAbsent('position[$i][lat]', () => point.latitude.toString());
        body.putIfAbsent('position[$i][long]', () => point.longitude.toString());
        i++;
      }
      dynamic resp = await ApiClient().postAPI(Constants().apiVersion + 'info_diseases/user_info_diseases', 'POST', BaseResponse(), body: body);
      if (resp.checkOK(passString: true)) {
        emit(SendDiseasesPositionState(resp));
      }
    });
    on<LoadDetailWeatherEvent>((event, emit) async {
      emit(const BaseState(isShowLoading: true));
      final response = await http.get(Uri.parse(Constants().baseUrl + Constants().apiVersion + 'weather/week_detail/${event.id}'));
      logDebug("======>${response.request}");
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        emit(LoadDetailWeatherState(data["data"], event.point));
        add(GetLocationEvent(event.point));
      }
    });
    on<LoadingAudioEvent>((event, emit) async {
      if (event.value) {
        emit(LoadingAudioState(true));
        await Future.delayed(const Duration(milliseconds: 10000));
      }
      emit(LoadingAudioState(false));
    });
    on<ChangeIndexTabEvent>((event, emit) => emit(ChangeIndexTabState()));
    on<ChangeProvinceEvent>((event, emit) => emit(ChangeProvinceState()));
    on<PlayAudioEvent>((event, emit) => emit(PlayAudioState(event.value)));
    on<ShowErrorEvent>((event, emit) => emit(ShowErrorState(event.error)));
  }

  bool _isValidCacheData(Map<String, dynamic> data) => data.containsKey('data') && data['data'] is Map<String, dynamic> && (data['data'] as Map<String, dynamic>).containsKey('features');

  bool _isRequestingInitialData(GetPetMapEvent event) => event.isSave == false && _isInitialRequestRunning && _initialRequestCompleter != null;

  Future<bool> _tryLoadFromCache(GetPetMapEvent event, Emitter<BaseState> emit) async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final file = File('${directory.path}/pet_map_cache.json');
      if (await file.exists()) {
        final String jsonString = await file.readAsString();
        final data = jsonDecode(jsonString) as Map<String, dynamic>;
        if (_isValidCacheData(data)) {
          logDebug("Sử dụng dữ liệu từ cache");
          emit(GetPetMapState(MapGeoJsonModel(data, sourceId: event.sourceId)));
          return true;
        }
      }
      return false;
    } catch (e) {
      logDebug("Lỗi khi đọc cache: $e");
      return false;
    }
  }

  Future<void> _waitForInitialRequest(Emitter<BaseState> emit) async {
    logDebug("Đang đợi lấy dữ liệu ban đầu...");
    final result = await _initialRequestCompleter!.future;
    emit(GetPetMapState(result));
  }

  void _prepareForApiCall(GetPetMapEvent event) {
    if (event.isSave == true) {
      _isInitialRequestRunning = true;
      _initialRequestCompleter = Completer<MapGeoJsonModel>();
      logDebug("Bắt đầu tải dữ liệu ban đầu...");
    } else {
      cancelCurrentRequest();
    }
  }

  Future<void> _savePetMapCache(Map<String, dynamic> data) async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      final file = File('${directory.path}/pet_map_cache.json');
      logDebug("Bắt đầu lưu dữ liệu vào cache: ${file.path}");
      final cacheDir = Directory(directory.path);
      if (!await cacheDir.exists()) {
        logDebug("Thư mục cache không tồn tại, tạo mới thư mục");
        await cacheDir.create(recursive: true);
      }
      await file.writeAsString(jsonEncode(data));
      logDebug("Lưu dữ liệu vào cache thành công: ${file.path}");
    } catch (e) {
      logDebug("Lỗi khi lưu dữ liệu vào cache: $e");
    }
  }
}
