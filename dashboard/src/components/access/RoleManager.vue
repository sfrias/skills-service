/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div class="role-manager">
    <div class="row p-3">
      <div class="col-12">
        <existing-user-input :suggest="true" :validate="true" :user-type="userType" :excluded-suggestions="userIds"
                             v-model="selectedUser" data-cy="existingUserInput"/>
      </div>
      <div class="col-12 pt-3">
        <b-button variant="outline-hc" @click="addUserRole" :disabled="!selectedUser"
                  class="h-100" v-skills="'AddAdmin'">
          {{ addUserLabel }} <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"
                 aria-hidden="true"></i>
        </b-button>
      </div>
      <div class="col-12" v-if="errNotification.enable">
        <b-alert data-cy="error-msg" variant="danger" class="mt-2" show dismissible>
          <i class="fa fa-exclamation-circle mr-1" aria-hidden="true"></i> <strong>Error!</strong>
          Request could not be completed! <strong>{{ errNotification.msg }}</strong>
        </b-alert>
      </div>
    </div>

    <skills-b-table :options="table.options"
                    :items="data"
                    @page-changed="pageChanged"
                    @page-size-changed="pageSizeChanged"
                    @sort-changed="sortTable"
                    data-cy="roleManagerTable">
      <template v-slot:cell(userId)="data">
        <div :data-cy="`userCell_${data.value}`">
          {{ getUserDisplay(data.item) }}

          <b-button-group class="float-right">
            <b-button v-if="notCurrentUser(data.value)" @click="deleteUserRoleConfirm(data.item)"
                      variant="outline-primary" :aria-label="`remove access role from user ${data.value}`"
                      data-cy="removeUserBtn">
              <i class="text-warning fas fa-trash" aria-hidden="true"/>
            </b-button>
            <span v-else v-b-tooltip.hover="'Can not remove myself. Sorry!!'">
                  <b-button variant="outline-primary" disabled
                            data-cy="removeUserBtn"
                            aria-label="cannot remove access role from yourself">
                    <i class="text-warning fas fa-trash" aria-hidden="true"/>
                  </b-button>
            </span>
          </b-button-group>
        </div>
      </template>
    </skills-b-table>

  </div>
</template>

<script>
  import SkillsBTable from '@/components/utils/table/SkillsBTable';
  import AccessService from './AccessService';
  import ExistingUserInput from '../utils/ExistingUserInput';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  // role constants
  const ROLE_APP_USER = 'ROLE_APP_USER';
  const ROLE_PROJECT_ADMIN = 'ROLE_PROJECT_ADMIN';
  const ROLE_SUPERVISOR = 'ROLE_SUPERVISOR';
  const ROLE_SUPER_DUPER_USER = 'ROLE_SUPER_DUPER_USER';

  export default {
    name: 'RoleManager',
    mixins: [MsgBoxMixin, NavigationErrorMixin],
    components: { SkillsBTable, ExistingUserInput },
    props: {
      projectId: {
        type: String,
        default: null,
      },
      role: {
        type: String,
        default: 'ROLE_PROJECT_ADMIN',
        validator: (value) => ([ROLE_APP_USER, ROLE_PROJECT_ADMIN, ROLE_SUPERVISOR, ROLE_SUPER_DUPER_USER].indexOf(value) >= 0),
      },
      roleDescription: {
        type: String,
        default: 'Project Administrator',
      },
      userType: {
        type: String,
        default: 'DASHBOARD',
      },
      id: {
        type: String,
        default: 'add-user-div',
      },
      addUserLabel: {
        type: String,
        required: false,
        default: 'Add User',
      },
      addRoleConfirmation: {
        type: Object,
        required: false,
        default: null,
        validator(value) {
          return value.msgText && value.titleText && value.okBtnText;
        },
      },
    },
    data() {
      return {
        // user roles table properties
        data: [],
        userIds: [],
        selectedUser: null,
        isSaving: false,
        errNotification: {
          enable: false,
          msg: '',
        },
        table: {
          options: {
            busy: true,
            bordered: false,
            outlined: true,
            stacked: 'md',
            sortBy: 'userId',
            sortDesc: false,
            fields: [
              {
                key: 'userId',
                label: this.roleDescription,
                sortable: true,
              },
            ],
            pagination: {
              hideUnnecessary: true,
              server: true,
              currentPage: 1,
              totalRows: 1,
              pageSize: 5,
              possiblePageSizes: [5, 10, 15, 20],
            },
            tableDescription: `${this.roleDescription} table`,
          },
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      pageChanged(pageNum) {
        this.table.options.pagination.currentPage = pageNum;
        this.loadData();
      },
      pageSizeChanged(newSize) {
        this.table.options.pagination.pageSize = newSize;
        this.loadData();
      },
      sortTable(sortContext) {
        this.table.options.sortBy = sortContext.sortBy;
        this.table.options.sortDesc = sortContext.sortDesc;

        // set to the first page
        this.table.options.pagination.currentPage = 1;
        this.loadData();
      },
      loadData() {
        this.table.options.busy = true;
        const pageParams = {
          limit: this.table.options.pagination.pageSize,
          ascending: !this.table.options.sortDesc,
          page: this.table.options.pagination.currentPage,
          orderBy: this.table.options.sortBy,
        };
        AccessService.getUserRoles(this.projectId, this.role, pageParams)
          .then((result) => {
            this.table.options.busy = false;
            this.data = result.data;
            this.table.options.pagination.totalRows = result.totalCount;
            this.userIds = result.data.map(({ userIdForDisplay }) => userIdForDisplay);
          });
      },
      userAdded(userRole) {
        this.data.push(userRole);
        this.userIds.push(userRole.userIdForDisplay);
      },
      deleteUserRoleConfirm(row) {
        const msg = `Are you absolutely sure you want to remove ${this.getUserDisplay(row)} as a ${this.roleDescription}?`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.deleteUserRole(row);
            }
          });
      },
      deleteUserRole(row) {
        this.table.options.busy = true;
        AccessService.deleteUserRole(row.projectId, row.userId, row.roleName)
          .then(() => {
            this.data = this.data.filter((item) => item.userId !== row.userId);
            this.userIds = this.userIds.filter((userId) => userId !== row.userIdForDisplay);
            this.$emit('role-deleted', { userId: row.userId, role: row.roleName });
            this.table.options.busy = false;
          });
      },
      notCurrentUser(userId) {
        return this.$store.getters.userInfo && userId !== this.$store.getters.userInfo.userId;
      },
      addUserRole() {
        if (this.addRoleConfirmation) {
          this.msgConfirm(this.addRoleConfirmation.msgText, this.addRoleConfirmation.titleText, this.addRoleConfirmation.okBtnText).then((ok) => {
            if (ok) {
              this.doAddUserRole();
            }
          });
        } else {
          this.doAddUserRole();
        }
      },
      doAddUserRole() {
        this.isSaving = true;
        this.table.options.busy = true;
        const pkiAuthenticated = this.$store.getters.isPkiAuthenticated;

        AccessService.saveUserRole(this.projectId, this.selectedUser, this.role, pkiAuthenticated)
          .then(() => {
            this.$emit('role-added', { userId: this.selectedUser.userId, role: this.role });
            this.loadData();
          }).catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'UserNotFound') {
              this.errNotification.msg = e.response.data.explanation;
              this.errNotification.enable = true;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
              this.handlePush({ name: 'ErrorPage', query: { errorMessage } });
            }
          })
          .finally(() => {
            this.isSaving = false;
            this.selectedUser = null;
          });
      },
      getUserDisplay(item) {
        return item.lastName && item.firstName ? `${item.firstName} ${item.lastName} (${item.userIdForDisplay})` : item.userIdForDisplay;
      },
    },
  };
</script>

<style scoped>
</style>

<style>
</style>
