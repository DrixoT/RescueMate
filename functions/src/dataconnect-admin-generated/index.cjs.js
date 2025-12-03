const { validateAdminArgs } = require('firebase-admin/data-connect');

const connectorConfig = {
  connector: 'example',
  serviceId: 'functions',
  location: 'us-central1'
};
exports.connectorConfig = connectorConfig;

function createUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateUser', inputVars, inputOpts);
}
exports.createUser = createUser;

function getEmergencySettingsForUser(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetEmergencySettingsForUser', undefined, inputOpts);
}
exports.getEmergencySettingsForUser = getEmergencySettingsForUser;

function updateSafeZone(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateSafeZone', inputVars, inputOpts);
}
exports.updateSafeZone = updateSafeZone;

function listEmergencyContacts(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListEmergencyContacts', undefined, inputOpts);
}
exports.listEmergencyContacts = listEmergencyContacts;

