import axios from 'axios';

export default {
  getSetting(projectId, settingName) {
    return axios.get(`/admin/projects/${projectId}/settings/${settingName}`)
      .then(remoteRes => remoteRes.data);
  },
  saveSetting(projectId, settingObj) {
    return axios.post(`/admin/projects/${projectId}/settings/${settingObj.setting}`, settingObj)
      .then(remoteRes => remoteRes.data);
  },
};