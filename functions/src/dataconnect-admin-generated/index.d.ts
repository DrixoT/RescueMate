import { ConnectorConfig, DataConnect, OperationOptions, ExecuteOperationResponse } from 'firebase-admin/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;


export interface AlertHistory_Key {
  id: UUIDString;
  __typename?: 'AlertHistory_Key';
}

export interface CreateUserData {
  user_insert: User_Key;
}

export interface CreateUserVariables {
  displayName: string;
  email?: string | null;
  firebaseAuthUid: string;
  phoneNumber?: string | null;
  photoUrl?: string | null;
}

export interface EmergencyContact_Key {
  id: UUIDString;
  __typename?: 'EmergencyContact_Key';
}

export interface EmergencySetting_Key {
  id: UUIDString;
  __typename?: 'EmergencySetting_Key';
}

export interface GetEmergencySettingsForUserData {
  emergencySettings: ({
    id: UUIDString;
    autoCallDelaySeconds?: number | null;
    autoCallEmergencyServices: boolean;
    emergencyMessage: string;
    sendLocationWithAlerts?: boolean | null;
  } & EmergencySetting_Key)[];
}

export interface ListEmergencyContactsData {
  emergencyContacts: ({
    id: UUIDString;
    name: string;
    phoneNumber: string;
    relationship?: string | null;
  } & EmergencyContact_Key)[];
}

export interface SafeZone_Key {
  id: UUIDString;
  __typename?: 'SafeZone_Key';
}

export interface UpdateSafeZoneData {
  safeZone_update?: SafeZone_Key | null;
}

export interface UpdateSafeZoneVariables {
  id: UUIDString;
  name?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  radiusMeters?: number | null;
  alertOnExit?: boolean | null;
  notifyContactsOnExit?: boolean | null;
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

/** Generated Node Admin SDK operation action function for the 'CreateUser' Mutation. Allow users to execute without passing in DataConnect. */
export function createUser(dc: DataConnect, vars: CreateUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateUserData>>;
/** Generated Node Admin SDK operation action function for the 'CreateUser' Mutation. Allow users to pass in custom DataConnect instances. */
export function createUser(vars: CreateUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateUserData>>;

/** Generated Node Admin SDK operation action function for the 'GetEmergencySettingsForUser' Query. Allow users to execute without passing in DataConnect. */
export function getEmergencySettingsForUser(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<GetEmergencySettingsForUserData>>;
/** Generated Node Admin SDK operation action function for the 'GetEmergencySettingsForUser' Query. Allow users to pass in custom DataConnect instances. */
export function getEmergencySettingsForUser(options?: OperationOptions): Promise<ExecuteOperationResponse<GetEmergencySettingsForUserData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateSafeZone' Mutation. Allow users to execute without passing in DataConnect. */
export function updateSafeZone(dc: DataConnect, vars: UpdateSafeZoneVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateSafeZoneData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateSafeZone' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateSafeZone(vars: UpdateSafeZoneVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateSafeZoneData>>;

/** Generated Node Admin SDK operation action function for the 'ListEmergencyContacts' Query. Allow users to execute without passing in DataConnect. */
export function listEmergencyContacts(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListEmergencyContactsData>>;
/** Generated Node Admin SDK operation action function for the 'ListEmergencyContacts' Query. Allow users to pass in custom DataConnect instances. */
export function listEmergencyContacts(options?: OperationOptions): Promise<ExecuteOperationResponse<ListEmergencyContactsData>>;

